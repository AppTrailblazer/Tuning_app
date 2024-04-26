package com.willeypianotuning.toneanalyzer.billing

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.PurchasesUpdatedListener

interface BillingClientFactory {
    fun create(listener: PurchasesUpdatedListener): BillingClient
}