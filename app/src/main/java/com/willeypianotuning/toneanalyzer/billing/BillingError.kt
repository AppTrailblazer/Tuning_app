package com.willeypianotuning.toneanalyzer.billing

import com.android.billingclient.api.BillingResult

class BillingError(val result: BillingResult) : RuntimeException(result.debugMessage) {
    constructor(code: Int, message: String) : this(
        BillingResult.newBuilder().setResponseCode(code).setDebugMessage(message).build()
    )
}