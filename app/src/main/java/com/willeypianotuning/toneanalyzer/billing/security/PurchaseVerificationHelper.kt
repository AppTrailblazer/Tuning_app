package com.willeypianotuning.toneanalyzer.billing.security

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import com.willeypianotuning.toneanalyzer.billing.InAppPurchase
import com.willeypianotuning.toneanalyzer.billing.VerificationStatus
import com.willeypianotuning.toneanalyzer.billing.sha256
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.tls.HandshakeCertificates
import org.json.JSONObject
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class PurchaseVerificationHelper @Inject constructor(
    @ApplicationContext private val appContext: Context
) {

    private val spec = ConnectionSpec.Builder(ConnectionSpec.COMPATIBLE_TLS)
        .tlsVersions(TlsVersion.TLS_1_3, TlsVersion.TLS_1_2, TlsVersion.TLS_1_1, TlsVersion.TLS_1_0)
        .cipherSuites(
            CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
            CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
            CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256,
            CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA,
            CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA,
            CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA,
            CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA,
            CipherSuite.TLS_ECDHE_ECDSA_WITH_RC4_128_SHA,
            CipherSuite.TLS_ECDHE_RSA_WITH_RC4_128_SHA,
            CipherSuite.TLS_DHE_RSA_WITH_AES_128_CBC_SHA,
            CipherSuite.TLS_DHE_DSS_WITH_AES_128_CBC_SHA,
            CipherSuite.TLS_DHE_RSA_WITH_AES_256_CBC_SHA
        )
        .build()

    private val certificates = HandshakeCertificates.Builder()
        .addPlatformTrustedCertificates()
        .addTrustedCertificate(CustomTrust.letsEncrypt_ISRG_Root_1)
        .addTrustedCertificate(CustomTrust.letsEncrypt_ISRG_Root_2)
        .build()

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .connectionSpecs(listOf(spec))
        .sslSocketFactory(certificates.sslSocketFactory(), certificates.trustManager)
        .retryOnConnectionFailure(true)
        .build()

    private fun generatePublicKey(): String {
        return reverseString("qkgBNAjIBIIM") + "hkiG9w0BAQEFAAOCAQ" + manipulateString(10) + reverseString2(
            "CbiIMa"
        ) + "gKCAQ" + "EAg+fh3RsvtBu/jDESgqFbA8xfrgTNXp" + manipulateString(11) + "oCA0f1fJgEq3Nrcy" + replaceString(
            "t+Io5/3f0rh387uf9dj239fIN"
        ) + "Jvs7KfuFytBc020ojVhqo+PYqokaaPJdgqmIBjT7JTx" + manipulateString(10) + "CTcZwuqKGFZUUbpvc5FpMliRzo8GO2jgJmHujinqTdsHjNhogYjqw8XwF3yA8r1kcI+jYZlYgW" + replaceString2(
            "HItgXtwo9jf8rhrtme1TGW/awUP5q3EGY8E+Oijejf93Fjemqx5VAF3r"
        ) + "XqAOdwVkrwED+5HF40AT4+b3LtjS1eflvHBaZip9KW4U" + manipulateString(11) + "KhfMQHgpxRjSaRFWVH0JTOIs122lilwGRYoKcHBdzhAf2/OaQIDAQAB"
    }

    private inline fun iabPreferences(): SharedPreferences {
        return appContext.getSharedPreferences("metadata.pt", Context.MODE_PRIVATE)
    }

    private fun decryptPurchase(value: String): String? {
        val key = generateEncryptionKey()
        val iv = "boKRNTYYNp7AiOvY"
        return kotlin.runCatching {
            SecurityUtils.Aes.Cbc.decrypt(value, key, iv)
        }.getOrElse {
            kotlin.runCatching {
                SecurityUtils.Aes.Ecb.decrypt(value, key)
            }.getOrNull()
        }
    }

    fun verifyPurchaseByCachedVerificationCode(purchase: InAppPurchase): InAppPurchase? {
        val prefs = iabPreferences()
        val purchaseKey = purchase.orderId.sha256()
        if (!prefs.contains(purchaseKey)) {
            Timber.d("Purchase [${purchase.orderId}] could not be verified with local cache, missing entry")
            return null
        }

        val content = decryptPurchase(prefs.getString(purchaseKey, "")!!) ?: return null
        val responseBody = JSONObject(content)
        if (responseBody.has("code")) {
            val code = responseBody.getInt("code")
            require(code == 200) { "Purchase could not be validated!" }
            var verifiedPurchase = purchase
            if (verifiedPurchase.isSubscription) {
                val expirationTimeMillis = responseBody.optLong("expiryTimeMillis", 0)
                val expirationDate = Date(expirationTimeMillis)
                if (expirationDate.before(Date())) {
                    Timber.d("Subscription verification expired. Verifying...")
                    return null
                }
                verifiedPurchase = verifiedPurchase.copy(expirationDate = expirationDate)
            }
            return verifiedPurchase.copy(verificationStatus = VerificationStatus.VERIFIED)
        }
        // if we have the new purchase information, but it is invalid
        // we can throw exception here without checking the old approach
        return null
    }

    @WorkerThread
    @VisibleForTesting
    fun verifyPurchaseOnServer(purchase: InAppPurchase, crypto: PurchaseCrypto): InAppPurchase? {
        val params = JSONObject()
        params.put("packageName", purchase.packageName)
        params.put("productId", purchase.products[0])
        params.put("token", purchase.purchaseToken)
        params.put("subscription", purchase.isSubscription)

        Timber.d("Verification request body for order %s: %s", purchase.orderId, params.toString())

        val requestBody = crypto.encrypt(params.toString())
        Timber.d("Encrypted request: $requestBody")

        val uri = Uri.parse("https://pianometer.com/iap/verify.php?encv=2")
            .buildUpon()
        for (entry in crypto.queryParams()) {
            uri.appendQueryParameter(entry.key, entry.value)
        }

        val request = Request.Builder()
            .url(uri.toString())
            .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
            .post(
                requestBody.toRequestBody("application/json".toMediaTypeOrNull())
            )
            .build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val encryptedResponseBody: String = response.body?.string() ?: ""
                val content = crypto.decrypt(encryptedResponseBody)
                Timber.d("Verification response body: %s", content)
                val responseBody = JSONObject(content)

                if (responseBody.has("code")) {
                    val code = responseBody.getInt("code")
                    check(code == 200) {
                        "Purchase could not be validated!"
                    }

                    var verifiedPurchase = purchase
                    if (verifiedPurchase.isSubscription) {
                        val expirationTimeMillis = responseBody.optLong("expiryTimeMillis", 0)
                        if (expirationTimeMillis == 0L) {
                            throw IllegalStateException("Could not verify subscription")
                        }
                        val expirationDate = Date(expirationTimeMillis)
                        verifiedPurchase = verifiedPurchase.copy(expirationDate = expirationDate)
                    }

                    Timber.d("Purchase [${purchase.orderId}] verified on the server. Updating verification cache")
                    iabPreferences().edit()
                        .putString(purchase.orderId.sha256(), encryptedResponseBody)
                        .apply()
                    return verifiedPurchase.copy(verificationStatus = VerificationStatus.VERIFIED)
                } else {
                    Timber.d("Failed to verify purchase. Malformed response")
                    return null
                }
            } else {
                Timber.d("Failed to connect to the verification server. Got HTTP code ${response.code}")
                return null
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to verify purchase")
            return null
        }
    }

    @WorkerThread
    suspend fun verifyPurchase(purchase: InAppPurchase): InAppPurchase {
        return withContext(Dispatchers.IO) {
            require(
                Security.verifyPurchase(
                    generatePublicKey(),
                    purchase.responseData,
                    purchase.signature
                )
            ) {
                "Signature verification failed"
            }
            var verifiedPurchase = verifyPurchaseByCachedVerificationCode(purchase)
            if (verifiedPurchase != null) {
                Timber.i("Purchase ${verifiedPurchase.orderId} verified via local cache")
                return@withContext verifiedPurchase
            }

            val crypto = CbcPurchaseCrypto(
                generateEncryptionKey(),
                "boKRNTYYNp7AiOvY"
            )
            verifiedPurchase = verifyPurchaseOnServer(purchase, crypto)

            if (verifiedPurchase != null) {
                return@withContext verifiedPurchase
            }

            throw IllegalStateException("Purchase could not be verified")
        }
    }

    @VisibleForTesting
    fun generateEncryptionKey(): String {
        return manipulateString(5) + reverseString2("xZgo") + "mr45" + manipulateString(3) + manipulateString(
            11
        ) + "CTF" + manipulateString(2) + reverseString("HTF1")
    }

    private fun reverseString(s: String): String {
        return StringBuilder(s).reverse().toString()
    }

    private fun reverseString2(s: String): String {
        return StringBuilder(s).reverse().toString().uppercase(Locale.US)
    }

    private fun manipulateString(i: Int): String {
        return if (i < 9) "" else (i - 2).toString()
    }

    private fun replaceString(s: String): String {
        return s.replace("3f0rh38", "+783+kZi").replace("f9dj239", "79rPRix")
    }

    private fun replaceString2(s: String): String {
        return s.replace("9jf8rhr", "Bp5FURnZ").replace("jejf93Fje", "HzjgCeRCQ")
    }

}
