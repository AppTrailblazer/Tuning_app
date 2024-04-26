package com.willeypianotuning.toneanalyzer.billing

object AppSkus {
    @JvmStatic
    val PRODUCT_SKU_PRO: String
        get() = "b3JwLnJlenlsYW5hZW5vdC5nbmludXRvbmFpcHllbGxpdy5tb2M=".decodeBase64().reversed()

    @JvmStatic
    val PRODUCT_SKU_PLUS: String
        get() = "c3VscC5yZXp5bGFuYWVub3QuZ25pbnV0b25haXB5ZWxsaXcubW9j".decodeBase64().reversed()

    @JvmStatic
    val PRODUCT_SKU_PLUS_TO_PRO: String
        get() = "b3Jwb3RzdWxwLnJlenlsYW5hZW5vdC5nbmludXRvbmFpcHllbGxpdy5tb2M=".decodeBase64()
            .reversed()

    @JvmStatic
    val IN_APP_SKUS: List<String>
        get() = listOf(PRODUCT_SKU_PLUS, PRODUCT_SKU_PLUS_TO_PRO, PRODUCT_SKU_PRO)

    @JvmStatic
    val SUBSCRIPTION_PRO: String
        get() = "b3JwLnNidXMucmV6eWxhbmFlbm90LmduaW51dG9uYWlweWVsbGl3Lm1vYw==".decodeBase64()
            .reversed()

    @JvmStatic
    val SUBS_SKUS: List<String>
        get() = listOf(SUBSCRIPTION_PRO)
}