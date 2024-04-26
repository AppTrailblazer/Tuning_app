package com.willeypianotuning.toneanalyzer.ui.upgrade.state

import com.android.billingclient.api.BillingResult
import com.willeypianotuning.toneanalyzer.billing.InAppPurchase

sealed class InAppPurchaseState {
    object NotSet : InAppPurchaseState()
    object Processing : InAppPurchaseState()
    class Error(val billingResult: BillingResult) : InAppPurchaseState()
    class Success(val purchases: List<InAppPurchase>) : InAppPurchaseState()
}