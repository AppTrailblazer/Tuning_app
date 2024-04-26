package com.willeypianotuning.toneanalyzer.billing

import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import timber.log.Timber
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private class SingleResumeContinuation<T>(
    private val continuation: Continuation<T>,
) : Continuation<T> {
    private var valueEmitted: Boolean = false
    override val context: CoroutineContext
        get() = continuation.context

    override fun resumeWith(result: Result<T>) {
        if (valueEmitted) {
            return
        }
        continuation.resumeWith(result)
        valueEmitted = true
    }
}

suspend fun BillingClient.connect(): Boolean = suspendCoroutine {
    // we just care about initial result of connect operation
    // so we emit only first resume call, and ignore all others (instead of crashing as in default impl),
    // we additionally check isReady in other places
    val continuation = SingleResumeContinuation(it)
    if (isReady) {
        it.resume(true)
        return@suspendCoroutine
    }

    startConnection(object : BillingClientStateListener {
        override fun onBillingSetupFinished(billingResult: BillingResult) {
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Timber.i("Successfully connected to Google Play Billing")
                continuation.resume(true)
            } else {
                Timber.i("Failed to connect to Google Play Billing. Code ${billingResult.responseCode} ${billingResult.debugMessage}")
                continuation.resumeWithException(BillingError(billingResult))
            }
        }

        override fun onBillingServiceDisconnected() {
            Timber.i("Google Play Billing connection lost")
            continuation.resumeWithException(
                BillingError(
                    BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
                    "Disconnected from Google Play"
                )
            )
        }
    })
}

suspend fun BillingClient.acknowledgePurchase(params: AcknowledgePurchaseParams): Int =
    suspendCoroutine { continuation ->
        if (!isReady) {
            continuation.resumeWithException(
                BillingError(
                    BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
                    "BillingClient is not ready"
                )
            )
            return@suspendCoroutine
        }

        acknowledgePurchase(params) { result ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                Timber.i("Purchase acknowledged successfully")
                continuation.resume(BillingClient.BillingResponseCode.OK)
            } else {
                continuation.resumeWithException(BillingError(result))
            }
        }
    }

suspend fun BillingClient.queryInAppProducts(): List<ProductDetails> =
    suspendCoroutine { continuation ->
        if (!isReady) {
            continuation.resumeWithException(
                BillingError(
                    BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
                    "BillingClient is not ready"
                )
            )
            return@suspendCoroutine
        }

        queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder().setProductList(AppSkus.IN_APP_SKUS.map {
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductType(BillingClient.ProductType.INAPP).setProductId(it).build()
            }).build()
        ) { billingResult, products ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                continuation.resume(products)
            } else {
                continuation.resumeWithException(BillingError(billingResult))
            }
        }
    }

suspend fun BillingClient.queryInAppSubscriptions(): List<ProductDetails> =
    suspendCoroutine { continuation ->
        if (!isReady) {
            continuation.resumeWithException(
                BillingError(
                    BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
                    "BillingClient is not ready"
                )
            )
            return@suspendCoroutine
        }

        queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder().setProductList(AppSkus.SUBS_SKUS.map {
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS).setProductId(it).build()
            }).build()
        ) { billingResult, products ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                continuation.resume(products)
            } else {
                continuation.resumeWithException(BillingError(billingResult))
            }
        }
    }

suspend fun BillingClient.queryAllProducts(): List<ProductDetails> {
    if (!isReady) {
        throw BillingError(
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE, "BillingClient is not ready"
        )
    }

    val products = queryInAppProducts()
    val subscriptions = queryInAppSubscriptions()

    return products + subscriptions
}

suspend fun BillingClient.queryInAppPurchases(): List<Purchase> = suspendCoroutine { continuation ->
    if (!isReady) {
        continuation.resumeWithException(
            BillingError(
                BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE, "BillingClient is not ready"
            )
        )
        return@suspendCoroutine
    }

    queryPurchasesAsync(
        QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
    ) { result, purchases ->
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            continuation.resume(purchases)
        } else {
            continuation.resumeWithException(BillingError(result))
        }
    }
}

suspend fun BillingClient.querySubsPurchases(): List<Purchase> = suspendCoroutine { continuation ->
    if (!isReady) {
        continuation.resumeWithException(
            BillingError(
                BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE, "BillingClient is not ready"
            )
        )
        return@suspendCoroutine
    }

    queryPurchasesAsync(
        QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
    ) { result, purchases ->
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            continuation.resume(purchases)
        } else {
            continuation.resumeWithException(BillingError(result))
        }
    }
}

suspend fun BillingClient.queryAllPurchases(): List<Purchase> {
    if (!isReady) {
        throw BillingError(
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE, "BillingClient is not ready"
        )
    }

    val purchases = queryInAppPurchases()
    val subscriptions = querySubsPurchases()

    return purchases + subscriptions
}