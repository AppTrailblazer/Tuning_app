package com.willeypianotuning.toneanalyzer.billing.security

data class PackageSignature(
    val packageName: String,
    val sha1Signature: String,
    val validateSignature: Boolean = true
)