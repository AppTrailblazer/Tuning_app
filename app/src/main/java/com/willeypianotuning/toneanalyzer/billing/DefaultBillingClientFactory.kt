package com.willeypianotuning.toneanalyzer.billing

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.PurchasesUpdatedListener

class DefaultBillingClientFactory(private val context: Context) : BillingClientFactory {
    override fun create(listener: PurchasesUpdatedListener): BillingClient {
        return BillingClient.newBuilder(context)
            .setListener(listener)
            .enablePendingPurchases()
            .build()
    }
}