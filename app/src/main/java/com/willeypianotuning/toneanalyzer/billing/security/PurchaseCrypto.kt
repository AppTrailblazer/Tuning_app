package com.willeypianotuning.toneanalyzer.billing.security

interface PurchaseCrypto {
    fun queryParams(): Map<String, String>
    fun encrypt(value: String): String
    fun decrypt(value: String): String
}

class EcbPurchaseCrypto(
    val key: String
) : PurchaseCrypto {
    override fun queryParams(): Map<String, String> = emptyMap()

    override fun encrypt(value: String): String {
        return SecurityUtils.Aes.Ecb.encrypt(value, key)
    }

    override fun decrypt(value: String): String {
        return SecurityUtils.Aes.Ecb.decrypt(value, key)
    }
}

class CbcPurchaseCrypto(
    val key: String,
    val iv: String
) : PurchaseCrypto {
    override fun queryParams(): Map<String, String> {
        return mapOf("encv" to "2")
    }

    override fun encrypt(value: String): String {
        return SecurityUtils.Aes.Cbc.encrypt(value, key, iv)
    }

    override fun decrypt(value: String): String {
        return SecurityUtils.Aes.Cbc.decrypt(value, key, iv)
    }
}