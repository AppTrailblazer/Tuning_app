package com.willeypianotuning.toneanalyzer.billing

import com.android.billingclient.api.Purchase
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date
import kotlin.test.assertEquals


class PurchaseStoreTest {
    private lateinit var encryptedPrefs: EncryptedPrefs
    private lateinit var sut: PurchaseStore

    @Before
    fun setUp() {
        encryptedPrefs = object : EncryptedPrefs {
            val map: MutableMap<String, String> = mutableMapOf()

            override fun putString(key: String, value: String) {
                map[key] = value
            }

            override fun putBoolean(key: String, value: Boolean) {
                map[key] = value.toString()
            }

            override fun getString(key: String, defValue: String): String {
                return map[key] ?: defValue
            }

            override fun getBoolean(key: String, defValue: Boolean): Boolean {
                return map[key]?.toBoolean() ?: defValue
            }
        }
        sut = PurchaseStore.createInstance(encryptedPrefs)
        assertTrue(sut.purchases.value.isEmpty())
    }

    private fun createPurchase(
        orderId: String,
        packageName: String = "com.willeypianotuning.toneanalyzer",
        products: List<String>,
        purchaseTime: Date = Date(),
        purchaseState: Int = Purchase.PurchaseState.PURCHASED,
        developerPayload: String = "",
        purchaseToken: String = "",
        signature: String = "",
        responseData: String = "",
        autoRenewing: Boolean = false,
        verificationStatus: VerificationStatus = VerificationStatus.NOT_VERIFIED,
    ) = InAppPurchase(
        orderId = orderId,
        packageName = packageName,
        products = products,
        purchaseTime = purchaseTime,
        purchaseState = purchaseState,
        developerPayload = developerPayload,
        purchaseToken = purchaseToken,
        signature = signature,
        responseData = responseData,
        autoRenewing = autoRenewing,
        acknowledged = false,
        verificationStatus = verificationStatus,
    )

    @Test
    fun `when has no purchases, isPlus returns false, isPro returns false`() {
        sut.updatePurchases(
            emptyList(), removeMissing = false
        )

        assertFalse(sut.isPlus)
        assertFalse(sut.isPro)
        assertFalse(sut.isProSubscription)
    }

    @Test
    fun `when has plus purchase, isPlus returns true, isPro returns false`() {
        sut.updatePurchases(
            listOf(
                createPurchase(
                    orderId = "order-1",
                    products = listOf(AppSkus.PRODUCT_SKU_PLUS),
                    purchaseState = Purchase.PurchaseState.PURCHASED,
                    verificationStatus = VerificationStatus.VERIFIED,
                ),
            ), removeMissing = false
        )

        assertTrue(sut.isPlus)
        assertFalse(sut.isPro)
        assertFalse(sut.isProSubscription)
    }

    @Test
    fun `when has plus and plus to pro purchase, isPlus returns true, isPro returns true`() {
        sut.updatePurchases(
            listOf(
                createPurchase(
                    orderId = "order-1",
                    products = listOf(AppSkus.PRODUCT_SKU_PLUS),
                    purchaseState = Purchase.PurchaseState.PURCHASED,
                    verificationStatus = VerificationStatus.VERIFIED,
                ),
                createPurchase(
                    orderId = "order-1",
                    products = listOf(AppSkus.PRODUCT_SKU_PLUS_TO_PRO),
                    purchaseState = Purchase.PurchaseState.PURCHASED,
                    verificationStatus = VerificationStatus.VERIFIED,
                ),
            ), removeMissing = false
        )

        assertTrue(sut.isPlus)
        assertTrue(sut.isPro)
        assertFalse(sut.isProSubscription)
    }

    @Test
    fun `when has only plus to pro purchase, isPlus returns false, isPro returns false`() {
        sut.updatePurchases(
            listOf(
                createPurchase(
                    orderId = "order-1",
                    products = listOf(AppSkus.PRODUCT_SKU_PLUS_TO_PRO),
                    purchaseState = Purchase.PurchaseState.PURCHASED,
                    verificationStatus = VerificationStatus.VERIFIED,
                ),
            ), removeMissing = false
        )

        assertFalse(sut.isPlus)
        assertFalse(sut.isPro)
        assertFalse(sut.isProSubscription)
    }

    @Test
    fun `when has pro purchase, isPlus returns true, isPro returns true`() {
        sut.updatePurchases(
            listOf(
                createPurchase(
                    orderId = "order-1",
                    products = listOf(AppSkus.PRODUCT_SKU_PRO),
                    purchaseState = Purchase.PurchaseState.PURCHASED,
                    verificationStatus = VerificationStatus.VERIFIED,
                ),
            ), removeMissing = false
        )

        assertTrue(sut.isPlus)
        assertTrue(sut.isPro)
        assertFalse(sut.isProSubscription)
    }

    @Test
    fun `when has pro subscription, isPlus returns true, isPro returns true`() {
        sut.updatePurchases(
            listOf(
                createPurchase(
                    orderId = "order-1",
                    products = listOf(AppSkus.SUBSCRIPTION_PRO),
                    purchaseState = Purchase.PurchaseState.PURCHASED,
                    verificationStatus = VerificationStatus.VERIFIED,
                ),
            ), removeMissing = false
        )

        assertTrue(sut.isPlus)
        assertTrue(sut.isPro)
        assertTrue(sut.isProSubscription)
    }

    @Test
    fun `when consume pending purchase, purchase is removed`() {
        sut.updatePurchases(
            listOf(
                createPurchase(
                    orderId = "",
                    products = listOf(AppSkus.PRODUCT_SKU_PRO),
                    purchaseState = Purchase.PurchaseState.PENDING
                ),
            ), removeMissing = false
        )
        assertFalse(sut.purchases.value.isEmpty())

        sut.consume(
            createPurchase(
                orderId = "",
                products = listOf(AppSkus.PRODUCT_SKU_PRO),
                purchaseState = Purchase.PurchaseState.PENDING
            ),
        )

        assertTrue(sut.purchases.value.isEmpty())
    }

    @Test
    fun `when consume finished purchase, purchase is removed`() {
        sut.updatePurchases(
            listOf(
                createPurchase(
                    orderId = "order-1",
                    products = listOf(AppSkus.PRODUCT_SKU_PRO),
                    purchaseState = Purchase.PurchaseState.PURCHASED,
                    verificationStatus = VerificationStatus.VERIFIED,
                ),
            ), removeMissing = false
        )
        assertFalse(sut.purchases.value.isEmpty())

        sut.consume(
            createPurchase(
                orderId = "order-1",
                products = listOf(AppSkus.PRODUCT_SKU_PRO),
                purchaseState = Purchase.PurchaseState.PURCHASED,
                verificationStatus = VerificationStatus.VERIFIED,
            ),
        )

        assertTrue(sut.purchases.value.isEmpty())
    }

    @Test
    fun `when updating purchase with removeMissing flag, any missing finished purchases removed`() {
        sut.updatePurchases(
            listOf(
                createPurchase(
                    orderId = "order-1",
                    products = listOf(AppSkus.PRODUCT_SKU_PLUS),
                    purchaseState = Purchase.PurchaseState.PURCHASED,
                    verificationStatus = VerificationStatus.VERIFIED,
                ),
                createPurchase(
                    orderId = "order-2",
                    products = listOf(AppSkus.PRODUCT_SKU_PLUS_TO_PRO),
                    purchaseState = Purchase.PurchaseState.PENDING,
                    verificationStatus = VerificationStatus.NOT_VERIFIED,
                ),
            ),
            removeMissing = false,
        )
        assertFalse(sut.purchases.value.isEmpty())

        sut.updatePurchases(
            listOf(
                createPurchase(
                    orderId = "order-2",
                    products = listOf(AppSkus.PRODUCT_SKU_PLUS_TO_PRO),
                    purchaseState = Purchase.PurchaseState.PURCHASED,
                    verificationStatus = VerificationStatus.VERIFIED,
                ),
            ),
            removeMissing = true,
        )

        val purchases = sut.purchases.value

        assertEquals(1, purchases.size)
        assertEquals("order-2", purchases[0].orderId)
        assertEquals(Purchase.PurchaseState.PURCHASED, purchases[0].purchaseState)
    }
}