package com.willeypianotuning.toneanalyzer.ui.upgrade

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.willeypianotuning.toneanalyzer.BuildConfig
import com.willeypianotuning.toneanalyzer.billing.BillingClientFactory
import com.willeypianotuning.toneanalyzer.billing.BillingError
import com.willeypianotuning.toneanalyzer.billing.InAppPurchase
import com.willeypianotuning.toneanalyzer.billing.PurchaseHandler
import com.willeypianotuning.toneanalyzer.billing.PurchaseStore
import com.willeypianotuning.toneanalyzer.billing.connect
import com.willeypianotuning.toneanalyzer.billing.queryAllProducts
import com.willeypianotuning.toneanalyzer.billing.queryAllPurchases
import com.willeypianotuning.toneanalyzer.billing.security.PurchaseVerificationHelper
import com.willeypianotuning.toneanalyzer.di.IoDispatcher
import com.willeypianotuning.toneanalyzer.ui.upgrade.state.InAppPurchaseState
import com.willeypianotuning.toneanalyzer.ui.upgrade.state.UpgradeScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@HiltViewModel
class UpgradeViewModel @Inject constructor(
    billingClientFactory: BillingClientFactory,
    private val purchaseStore: PurchaseStore,
    verificationHelper: PurchaseVerificationHelper,
    @IoDispatcher private val ioDispatcher: CoroutineContext
) : ViewModel(), PurchasesUpdatedListener {
    private val billingClient = billingClientFactory.create(this)
    private val purchaseHandler = PurchaseHandler(billingClient, purchaseStore, verificationHelper)

    private val _screenState = MutableLiveData<UpgradeScreenState>(null)
    val screenState: LiveData<UpgradeScreenState> get() = _screenState

    private val _purchaseState = MutableLiveData<InAppPurchaseState>(InAppPurchaseState.NotSet)
    val purchaseState: LiveData<InAppPurchaseState> get() = _purchaseState

    fun loadData() {
        Timber.d("Connecting to BillingClient")
        viewModelScope.launch {
            kotlin.runCatching {
                _screenState.postValue(UpgradeScreenState.Loading)
                billingClient.connect()
                Timber.d("Querying purchases")
                val purchases = billingClient.queryAllPurchases()
                Timber.d("Processing purchases")
                purchaseHandler.handlePurchases(
                    purchases.map { InAppPurchase(it) },
                    removeMissing = true,
                )
                Timber.d("Querying products")
                val products = billingClient.queryAllProducts()
                Timber.d("Billing initialization successful")
                val supportsSubscriptions =
                    billingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS).responseCode == BillingClient.BillingResponseCode.OK
                _screenState.postValue(UpgradeScreenState.Success(products, supportsSubscriptions))
            }.onFailure {
                Timber.d("Billing initialization failed")
                if (it is BillingError) {
                    _screenState.postValue(UpgradeScreenState.Error(it.result))
                } else {
                    _screenState.postValue(
                        UpgradeScreenState.Error(
                            BillingResult.newBuilder()
                                .setResponseCode(BillingClient.BillingResponseCode.ERROR)
                                .setDebugMessage(it.message ?: "Unknown error").build()
                        )
                    )
                }
            }
        }
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult, purchases: MutableList<Purchase>?
    ) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            viewModelScope.launch {
                kotlin.runCatching {
                    withContext(ioDispatcher) {
                        _purchaseState.postValue(InAppPurchaseState.Processing)
                        val inAppPurchases = requireNotNull(purchases).map { InAppPurchase(it) }
                        purchaseHandler.handlePurchases(
                            inAppPurchases,
                            removeMissing = false,
                        )
                        _purchaseState.postValue(InAppPurchaseState.Success(inAppPurchases))
                        Timber.d("Purchases updated successfully")
                    }
                }.onFailure {
                    if (it is BillingError) {
                        _purchaseState.postValue(InAppPurchaseState.Error(it.result))
                    } else {
                        _purchaseState.postValue(
                            InAppPurchaseState.Error(
                                BillingResult.newBuilder()
                                    .setResponseCode(BillingClient.BillingResponseCode.ERROR)
                                    .setDebugMessage(it.message ?: "Unknown error").build()
                            )
                        )
                    }
                    Timber.e(it, "Failed to update purchases")
                }
            }
        } else {
            _purchaseState.postValue(InAppPurchaseState.Error(billingResult))
            Timber.d("PurchaseUpdate received with error ${billingResult.responseCode} ${billingResult.debugMessage}")
        }
    }

    fun purchase(activity: AppCompatActivity, sku: String) {
        val state = _screenState.value
        if (state !is UpgradeScreenState.Success) {
            return
        }

        if (purchaseState.value is InAppPurchaseState.Processing) {
            return
        }

        val existingPurchase =
            purchaseStore.purchases.value.firstOrNull { it.products.contains(sku) }
        if (existingPurchase != null && !existingPurchase.isExpiredSubscription) {
            _purchaseState.postValue(
                InAppPurchaseState.Error(
                    BillingResult.newBuilder()
                        .setResponseCode(BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED)
                        .setDebugMessage("Already purchased").build()
                )
            )
            return
        }

        if (!billingClient.isReady) {
            _purchaseState.postValue(
                InAppPurchaseState.Error(
                    BillingResult.newBuilder()
                        .setResponseCode(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED)
                        .setDebugMessage("Connection to Google Play Billing Service is lost")
                        .build()
                )
            )
            return
        }

        val skuDetails = state.products.firstOrNull { it.productId == sku }
        if (skuDetails == null) {
            _purchaseState.postValue(
                InAppPurchaseState.Error(
                    BillingResult.newBuilder()
                        .setResponseCode(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE)
                        .setDebugMessage("Product not found").build()
                )
            )
            return
        }

        val offerToken = skuDetails.subscriptionOfferDetails?.getOrNull(0)?.offerToken
        if (skuDetails.productType == BillingClient.ProductType.SUBS) {
            val subSupport =
                billingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
            if (subSupport.responseCode != BillingClient.BillingResponseCode.OK) {
                _purchaseState.postValue(
                    InAppPurchaseState.Error(subSupport)
                )
                return
            }

            if (offerToken == null) {
                Timber.w(UnsupportedOperationException("No offer token available for ${skuDetails.productId}"))
                _purchaseState.postValue(
                    InAppPurchaseState.Error(
                        BillingResult.newBuilder()
                            .setResponseCode(BillingClient.BillingResponseCode.ERROR)
                            .setDebugMessage("Offer token not found").build()
                    )
                )
                return
            }
        }

        Timber.d("Creating purchase request for ${skuDetails.productId}")
        val purchaseRequestBuilder =
            BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(skuDetails)
        offerToken?.let { purchaseRequestBuilder.setOfferToken(it) }

        val result = billingClient.launchBillingFlow(
            activity, BillingFlowParams.newBuilder().setProductDetailsParamsList(
                    listOf(purchaseRequestBuilder.build())
                ).build()
        )
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _purchaseState.postValue(InAppPurchaseState.Error(result))
        } else {
            Timber.i("Purchase request initiated successfully")
        }
    }

    /**
     * To be used only in DEBUG mode
     */
    fun consume(purchase: InAppPurchase) {
        if (!BuildConfig.DEBUG) {
            return
        }

        if (!billingClient.isReady) {
            return
        }

        billingClient.consumeAsync(
            ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        ) { billingResult, purchaseToken ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                purchaseStore.consume(purchase)
            } else if (billingResult.responseCode == BillingClient.BillingResponseCode.ITEM_NOT_OWNED) {
                // two request were initialized probably, should remove the purchase now
                purchaseStore.consume(purchase)
            } else {
                Timber.e("Failed to consume purchase: ${billingResult.debugMessage}. Token = $purchaseToken")
            }
        }
    }

    fun onPurchaseResultDelivered() {
        _purchaseState.postValue(InAppPurchaseState.NotSet)
    }

    override fun onCleared() {
        super.onCleared()
        billingClient.endConnection()
    }
}