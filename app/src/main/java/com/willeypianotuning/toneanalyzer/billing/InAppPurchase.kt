package com.willeypianotuning.toneanalyzer.billing

import com.android.billingclient.api.Purchase
import java.util.Date

data class InAppPurchase(
    val orderId: String,
    val packageName: String,
    val products: List<String>,
    val purchaseTime: Date,
    @Purchase.PurchaseState
    val purchaseState: Int,
    val developerPayload: String,
    val purchaseToken: String,
    val signature: String,
    val responseData: String,
    val autoRenewing: Boolean,
    val acknowledged: Boolean,
    var verificationStatus: VerificationStatus = VerificationStatus.NOT_VERIFIED,
    var expirationDate: Date? = null
) {
    constructor(p: Purchase, expirationDate: Date? = null) : this(
        p.orderId ?: "",
        p.packageName,
        p.products,
        Date(p.purchaseTime),
        p.purchaseState,
        p.developerPayload,
        p.purchaseToken,
        p.signature,
        p.originalJson,
        p.isAutoRenewing,
        p.isAcknowledged,
        VerificationStatus.NOT_VERIFIED,
        expirationDate
    )

    inline val isPurchased: Boolean get() = purchaseState == Purchase.PurchaseState.PURCHASED

    inline val isPending: Boolean get() = purchaseState == Purchase.PurchaseState.PENDING

    inline val isVerified: Boolean get() = verificationStatus == VerificationStatus.VERIFIED

    inline val isSubscription: Boolean
        get() = AppSkus.SUBS_SKUS.toSet().intersect(products.toSet()).isNotEmpty()

    inline val isExpired: Boolean get() = expirationDate?.before(Date()) == true

    inline val isExpiredSubscription: Boolean get() = isSubscription && isExpired

    fun isSame(to: InAppPurchase): Boolean {
        if (orderId.isNotEmpty() && to.orderId.isNotEmpty()) {
            return orderId == to.orderId
        }

        if (products.size != to.products.size) {
            return false
        }

        return products.subtract(to.products.toSet()).isEmpty()
    }
}