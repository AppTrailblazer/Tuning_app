package com.willeypianotuning.toneanalyzer.billing

import androidx.annotation.UiThread
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.willeypianotuning.toneanalyzer.billing.security.PurchaseVerificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PurchaseRestoreTask @Inject constructor(
    private val billingClientFactory: BillingClientFactory,
    private val purchaseStore: PurchaseStore,
    private val purchaseVerificationHelper: PurchaseVerificationHelper
) {
    private var billingClient: BillingClient? = null
    private val restoreScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var disposable: Job? = null
    private val runningRestore = AtomicBoolean(false)

    private val listener = PurchasesUpdatedListener { result, purchases ->
        // ignored
    }

    fun shouldRestore(): Boolean {
        if (runningRestore.get()) {
            return false
        }
        val purchases = purchaseStore.purchases.value
        val hasPending = purchases.any { it.purchaseState == Purchase.PurchaseState.PENDING }
        val hasNotAcknowledged =
            purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.acknowledged }
        val hasNotVerified =
            purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED && it.verificationStatus != VerificationStatus.VERIFIED }
        val hasExpired = purchases.any {
            it.purchaseState == Purchase.PurchaseState.PURCHASED && it.expirationDate?.before(Date()) == true
        }
        if (hasPending || hasNotAcknowledged || hasNotVerified || hasExpired) {
            Timber.i("P = $hasPending A = $hasNotAcknowledged V = $hasNotVerified E = $hasExpired")
            return true
        }
        val timeSinceLastUpdate = Date().time - purchaseStore.lastUpdateTime.time
        Timber.d("Time since last update = $timeSinceLastUpdate")
        return timeSinceLastUpdate > TimeUnit.DAYS.toMillis(1)
    }

    @UiThread
    fun restore() {
        if (runningRestore.get()) {
            return
        }

        runningRestore.getAndSet(true)
        val billingClient = billingClientFactory.create(listener)
        this.billingClient = billingClient

        val purchaseHandler = PurchaseHandler(
            billingClient,
            purchaseStore,
            purchaseVerificationHelper
        )
        Timber.d("Connecting to BillingClient")
        disposable = restoreScope.launch {
            kotlin.runCatching {
                billingClient.connect()
                Timber.d("Querying purchases")
                val purchases = billingClient.queryAllPurchases()
                Timber.d("Processing purchases")
                purchaseHandler.handlePurchases(
                    purchases.map { InAppPurchase(it) },
                    removeMissing = true
                )
                billingClient.endConnection()
                runningRestore.getAndSet(false)
                Timber.d("Billing initialization successful")
            }.onFailure {
                Timber.e(it, "Billing initialization failed")
            }
        }
    }
}