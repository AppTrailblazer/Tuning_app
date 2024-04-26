package com.willeypianotuning.toneanalyzer.billing

import android.content.Context
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import com.android.billingclient.api.Purchase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.util.*

class PurchaseStore private constructor(
    private val iabPreferences: EncryptedPrefs,
) {
    companion object {
        private var _instance: PurchaseStore? = null

        @UiThread
        fun getInstance(context: Context): PurchaseStore {
            return _instance ?: createInstance(
                EcbEncryptedPrefs("metadata.pt", context.applicationContext),
            ).apply {
                _instance = this
            }
        }

        @UiThread
        @VisibleForTesting
        fun createInstance(
            iabPreferences: EncryptedPrefs,
        ): PurchaseStore {
            return _instance ?: PurchaseStore(
                iabPreferences,
            ).apply {
                this.restorePurchases()
            }
        }
    }

    private val purchaseSerializer = PurchaseSerializer()

    private val _purchases = MutableStateFlow<List<InAppPurchase>>(emptyList())
    val purchases: StateFlow<List<InAppPurchase>> get() = _purchases

    var lastUpdateTime: Date = Date(0)
        private set

    @Suppress("unused")
    private fun handleExpiration() {
        val existingPurchases = _purchases.value.toMutableList()
        val expired = existingPurchases.filter {
            it.purchaseState == Purchase.PurchaseState.PURCHASED && it.expirationDate?.before(Date()) == true
        }.map { it.orderId }
        if (expired.isEmpty()) {
            return
        }
        existingPurchases.removeAll { expired.contains(it.orderId) }
        _purchases.tryEmit(existingPurchases)
        lastUpdateTime = Date()
        savePurchases(existingPurchases)
    }

    fun updatePurchases(purchases: List<InAppPurchase>, removeMissing: Boolean) {
        val existingPurchases = _purchases.value.toMutableList()
        for (purchase in purchases) {
            existingPurchases.removeAll { it.isSame(purchase) }
        }
        if (removeMissing) {
            val orderIds = purchases.filter { it.isPurchased }.map { it.orderId }.toSet()
            existingPurchases.removeAll {
                it.isPurchased && !orderIds.contains(it.orderId)
            }
        }
        existingPurchases.addAll(purchases)
        _purchases.tryEmit(existingPurchases)
        lastUpdateTime = Date()
        savePurchases(existingPurchases)
    }

    fun consume(purchase: InAppPurchase) {
        val existingPurchases = _purchases.value.toMutableList()
        existingPurchases.removeAll { it.isSame(purchase) }
        _purchases.tryEmit(existingPurchases)
        savePurchases(existingPurchases)
    }

    @UiThread
    private fun restorePurchases() {
        kotlin.runCatching {
            val cache = iabPreferences.getString("purchaseCache", "")
            if (cache.isEmpty()) {
                return
            }
            val root = JSONObject(cache)
            val purchasesArray = root.getJSONArray("purchases")
            val purchases = List(purchasesArray.length()) {
                purchaseSerializer.fromJson(purchasesArray.getJSONObject(it))
            }
            this.lastUpdateTime = Date(root.getLong("time"))
            this._purchases.value = purchases
        }.onFailure {
            Timber.e(it, "Failed to restore purchases")
        }
    }

    private fun savePurchases(purchases: List<InAppPurchase>) {
        kotlin.runCatching {
            val root = JSONObject()
            val purchasesArray = JSONArray()
            for (purchase in purchases) {
                purchasesArray.put(purchaseSerializer.toJson(purchase))
            }
            root.put("purchases", purchasesArray)
            root.put("time", lastUpdateTime.time)

            iabPreferences.putString("purchaseCache", root.toString())
        }.onFailure {
            Timber.e(it, "Failed to save purchases")
        }
    }

    fun plusState(): Int {
        if (getPurchaseState(AppSkus.PRODUCT_SKU_PLUS) != Purchase.PurchaseState.UNSPECIFIED_STATE) {
            return getPurchaseState(AppSkus.PRODUCT_SKU_PLUS)
        }
        if (getPurchaseState(AppSkus.PRODUCT_SKU_PRO) == Purchase.PurchaseState.PURCHASED) {
            return Purchase.PurchaseState.PURCHASED
        }
        if (getPurchaseState(AppSkus.SUBSCRIPTION_PRO) == Purchase.PurchaseState.PURCHASED) {
            return Purchase.PurchaseState.PURCHASED
        }
        return Purchase.PurchaseState.UNSPECIFIED_STATE
    }

    fun proState(): Int {
        if (getPurchaseState(AppSkus.PRODUCT_SKU_PRO) != Purchase.PurchaseState.UNSPECIFIED_STATE) {
            return getPurchaseState(AppSkus.PRODUCT_SKU_PRO)
        }
        if (getPurchaseState(AppSkus.SUBSCRIPTION_PRO) != Purchase.PurchaseState.UNSPECIFIED_STATE) {
            return getPurchaseState(AppSkus.SUBSCRIPTION_PRO)
        }
        if (getPurchaseState(AppSkus.PRODUCT_SKU_PLUS) == Purchase.PurchaseState.PURCHASED && getPurchaseState(
                AppSkus.PRODUCT_SKU_PLUS_TO_PRO
            ) != Purchase.PurchaseState.UNSPECIFIED_STATE
        ) {
            return getPurchaseState(AppSkus.PRODUCT_SKU_PLUS_TO_PRO)
        }
        return Purchase.PurchaseState.UNSPECIFIED_STATE
    }

    inline val isPlus: Boolean get() = plusState() == Purchase.PurchaseState.PURCHASED

    inline val isPro: Boolean get() = proState() == Purchase.PurchaseState.PURCHASED

    val hasPendingPurchases: Boolean
        get() = _purchases.value.any { it.purchaseState == Purchase.PurchaseState.PENDING }

    val isProSubscription: Boolean
        get() {
            return getPurchaseState(AppSkus.SUBSCRIPTION_PRO) == Purchase.PurchaseState.PURCHASED
        }

    private fun getPurchaseState(sku: String): Int {
        val skuPurchases = this.purchases.value.filter { it.products.contains(sku) }
        if (skuPurchases.isEmpty()) {
            return Purchase.PurchaseState.UNSPECIFIED_STATE
        }

        val completedPurchase =
            skuPurchases.firstOrNull { it.isPurchased && it.isVerified && !it.isExpiredSubscription }
        if (completedPurchase != null) {
            return Purchase.PurchaseState.PURCHASED
        }
        val pendingPurchase = skuPurchases.firstOrNull { it.isPending }
        if (pendingPurchase != null) {
            return Purchase.PurchaseState.PENDING
        }

        return Purchase.PurchaseState.UNSPECIFIED_STATE
    }
}