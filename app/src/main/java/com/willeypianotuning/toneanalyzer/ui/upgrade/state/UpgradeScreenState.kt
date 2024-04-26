package com.willeypianotuning.toneanalyzer.ui.upgrade.state

import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails

sealed class UpgradeScreenState {
    object Loading : UpgradeScreenState()
    class Error(val billingResult: BillingResult) : UpgradeScreenState()
    class Success(val products: List<ProductDetails>, val supportsSubscriptions: Boolean) :
        UpgradeScreenState()
}