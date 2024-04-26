package com.willeypianotuning.toneanalyzer.billing.security

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest
import java.util.*

class PackageSignatureChecker(private val context: Context) {

    private fun getSignatures(context: Context): List<String> {
        val signatureList: List<String>
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // New signature
                val sig = context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                ).signingInfo
                signatureList = if (sig.hasMultipleSigners()) {
                    // Send all with apkContentsSigners
                    sig.apkContentsSigners.map { computeSha1(it.toByteArray()) }
                } else {
                    // Send one with signingCertificateHistory
                    sig.signingCertificateHistory.map { computeSha1(it.toByteArray()) }
                }
            } else {
                @Suppress("DEPRECATION")
                @SuppressLint("PackageManagerGetSignatures")
                val sig = context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                ).signatures
                signatureList = sig.map { computeSha1(it.toByteArray()) }
            }

            return signatureList
        } catch (e: Exception) {
            // Handle error
        }
        return emptyList()
    }


    fun isAnyOfSignaturesValid(packages: List<PackageSignature>): Boolean {
        val signatures = getSignatures(context)
        val sha1 = signatures.first()

        val packageName = context.packageName
        for (ps in packages) {
            if (ps.packageName != packageName) {
                continue
            }

            if (!ps.validateSignature) {
                return true
            }

            val signature = ps.sha1Signature.replace(":", "").uppercase(Locale.US)

            if (signature != sha1) {
                continue
            }

            return true
        }
        return false
    }

    private fun computeSha1(sig: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA" + 1.toString())
        digest.update(sig)
        val hashText = digest.digest()
        return bytesToHex(hashText)
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexArray = charArrayOf(
            '0',
            '1',
            '2',
            '3',
            '4',
            '5',
            '6',
            '7',
            '8',
            '9',
            'A',
            'B',
            'C',
            'D',
            'E',
            'F'
        )
        val hexChars = CharArray(bytes.size * 2)
        var v: Int
        for (j in bytes.indices) {
            v = (bytes[j].toInt() and 0xFF)
            hexChars[j * 2] = hexArray[v.ushr(4)]
            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }
}