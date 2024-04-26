package com.willeypianotuning.toneanalyzer.billing.security

import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import com.android.billingclient.api.Purchase
import com.willeypianotuning.toneanalyzer.TuningApplication
import com.willeypianotuning.toneanalyzer.billing.InAppPurchase
import com.willeypianotuning.toneanalyzer.billing.VerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.*

class PurchaseVerificationHelperTest {

    private lateinit var verificationHelper: PurchaseVerificationHelper

    private val purchase = InAppPurchase(
            orderId = "GPA.3383-0467-5423-56500",
            packageName = "com.willeypianotuning.toneanalyzer",
            sku = "com.willeypianotuning.toneanalyzer.pro",
            purchaseToken = "ifhdccjkgdjbffmekepklijp.AO-J1OzlWwUH_RO8JhWYN3dHCCdob7QTIhb6fIcrgTQyfunECo_QRUm9dKCSE4TGTV35pvwAb9s89VUSLi-8sTY38XRo0XjQiCoxY7rEhYRVosd0Vr05IDCTmw99obNxRJbTr6S7stuppwH1JrYcz2PwVt2KaQCGNB1modfsi8BWm8pmA--YPnA",
            purchaseTime = Date(),
            purchaseState = Purchase.PurchaseState.PURCHASED,
            developerPayload = "",
            signature = "",
            responseData = "",
            autoRenewing = false,
            acknowledged = true,
            verificationStatus = VerificationStatus.NOT_VERIFIED,
            expirationDate = null
    )

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<TuningApplication>()
        verificationHelper = PurchaseVerificationHelper(app)
    }

    @Test
    @LargeTest
    fun testPurchaseVerificationUsingMcryptWorks() {
        val crypto = EcbPurchaseCrypto(verificationHelper.generateEncryptionKey())
        val verifiedPurchase = verificationHelper.verifyPurchaseOnServer(purchase, crypto)
                ?: throw AssertionError("Purchase verification failed")
        assertEquals(VerificationStatus.VERIFIED, verifiedPurchase.verificationStatus)
    }

    @Test
    @LargeTest
    fun testPurchaseVerificationUsingOpenSslWorks() {
        val crypto = CbcPurchaseCrypto(verificationHelper.generateEncryptionKey(), "boKRNTYYNp7AiOvY")
        val verifiedPurchase = verificationHelper.verifyPurchaseOnServer(purchase, crypto)
                ?: throw AssertionError("Purchase verification failed")
        assertEquals(VerificationStatus.VERIFIED, verifiedPurchase.verificationStatus)
    }
}