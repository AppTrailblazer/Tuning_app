package com.willeypianotuning.toneanalyzer.billing

import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.willeypianotuning.toneanalyzer.billing.security.PurchaseVerificationHelper
import kotlinx.coroutines.runBlocking
import timber.log.Timber

class PurchaseHandler(
    private val billingClient: BillingClient,
    private val purchaseStore: PurchaseStore,
    private val verificationHandler: PurchaseVerificationHelper
) {

    fun handlePurchases(purchases: List<InAppPurchase>, removeMissing: Boolean) {
        val validPurchases = arrayListOf<InAppPurchase>()
        for (purchase in purchases) {
            Timber.i("Handling purchase ${purchase.orderId} ${purchase.products.joinToString(", ")} ${purchase.purchaseState}")
            handlePurchase(purchase)?.let {
                if (it.isSubscription && it.verificationStatus == VerificationStatus.VERIFIED && it.expirationDate == null) {
                    Timber.w("Subscription has no expiration date")
                }
                validPurchases.add(it)
            }
        }
        purchaseStore.updatePurchases(validPurchases, removeMissing)
    }

    private fun handlePurchase(purchase: InAppPurchase): InAppPurchase? {
        if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            return purchase
        }
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            val verifiedPurchase = verifyPurchase(purchase);
            if (verifiedPurchase.verificationStatus != VerificationStatus.VERIFIED) {
                return verifiedPurchase
            }
            if (verifiedPurchase.acknowledged) {
                return verifiedPurchase
            }
            return acknowledgePurchase(verifiedPurchase)
        }
        return null
    }

    private fun acknowledgePurchase(purchase: InAppPurchase): InAppPurchase {
        return kotlin.runCatching {
            Timber.i("Acknowledging purchase [${purchase.orderId}]")
            runBlocking {
                billingClient.acknowledgePurchase(
                    AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                )
                purchase.copy(acknowledged = true)
            }
        }.onFailure {
            Timber.e(it, "Failed to acknowledge purchase [${purchase.orderId}]")
        }.getOrDefault(purchase)
    }

    private fun verifyPurchase(purchase: InAppPurchase): InAppPurchase {
        return kotlin.runCatching {
            Timber.i("Verifying purchase [${purchase.orderId}]")
            runBlocking {
                verificationHandler.verifyPurchase(purchase)
            }
        }.onFailure {
            Timber.e(it, "Failed to validate purchase [${purchase.orderId}]")
        }.getOrDefault(purchase.copy(verificationStatus = VerificationStatus.VERIFICATION_FAILED))
    }
}