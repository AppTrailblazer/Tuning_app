package com.willeypianotuning.toneanalyzer.ui.upgrade

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.willeypianotuning.toneanalyzer.BuildConfig
import com.willeypianotuning.toneanalyzer.R
import com.willeypianotuning.toneanalyzer.billing.AppSkus
import com.willeypianotuning.toneanalyzer.billing.InAppPurchase
import com.willeypianotuning.toneanalyzer.billing.VerificationStatus
import com.willeypianotuning.toneanalyzer.ui.commons.BaseActivity
import com.willeypianotuning.toneanalyzer.ui.upgrade.state.InAppPurchaseState
import com.willeypianotuning.toneanalyzer.ui.upgrade.state.UpgradeScreenState
import com.willeypianotuning.toneanalyzer.utils.NetworkUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@AndroidEntryPoint
class UpgradeActivity : BaseActivity() {
    private val plusButton by lazy { findViewById<View>(R.id.upgrade_plus_button) }
    private val plusPurchaseUpgradeText by lazy { findViewById<TextView>(R.id.plusPurchaseUpgradeText) }
    private val plusPurchasePriceText by lazy { findViewById<TextView>(R.id.plusPurchasePriceText) }
    private val proButton by lazy { findViewById<View>(R.id.upgrade_pro_button) }
    private val proPurchaseUpgradeText by lazy { findViewById<TextView>(R.id.proPurchaseUpgradeText) }
    private val proPurchasePriceText by lazy { findViewById<TextView>(R.id.proPurchasePriceText) }
    private val proSubscriptionButton by lazy { findViewById<View>(R.id.upgrade_pro_subscription_button) }
    private val proSubscriptionUpgradeText by lazy { findViewById<TextView>(R.id.proSubscriptionUpgradeText) }
    private val proSubscriptionPriceText by lazy { findViewById<TextView>(R.id.proSubscriptionPriceText) }
    private val promoTextView by lazy { findViewById<TextView>(R.id.promoTextView) }
    private val plusOptions: List<PlanOptionView> by lazy { findPlanOptions(findViewById(R.id.plusPlanCard)) }
    private val proOptions: List<PlanOptionView> by lazy { findPlanOptions(findViewById(R.id.proPlanCard)) }

    private val viewModel: UpgradeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upgrade)
        setupToolbar()
        setTitle(R.string.activity_upgrade_title)

        plusButton.setOnClickListener { onPlusUpgradeClicked() }
        plusPurchasePriceText.text = getString(R.string.activity_upgrade_price_loading)
        proPurchasePriceText.text = getString(R.string.activity_upgrade_price_loading)
        proButton.setOnClickListener { onProUpgradeClicked() }
        proSubscriptionPriceText.text = getString(R.string.activity_upgrade_price_loading)
        proSubscriptionButton.setOnClickListener { onProSubscriptionUpgradeClicked() }

        viewModel.screenState.observe(this) { state ->
            if (state == null) {
                loadData()
                return@observe
            }

            if (state is UpgradeScreenState.Loading) {
                updatePurchaseButtons()
                return@observe
            }
            if (state is UpgradeScreenState.Error) {
                handleBillingError(state.billingResult)
                return@observe
            }
            if (state is UpgradeScreenState.Success) {
                updatePurchaseButtons()
                checkForPromotionalTexts()
                if ((purchaseStore.purchases.value
                        ?: emptyList()).any { it.purchaseState == Purchase.PurchaseState.PURCHASED && it.verificationStatus == VerificationStatus.VERIFICATION_FAILED }
                ) {
                    alert(getString(R.string.activity_upgrade_error_purchase_verification_failed)) {
                        loadData()
                    }
                }
                return@observe
            }
        }
        viewModel.purchaseState.observe(this) { state ->
            onPurchaseStateChanged(state)
        }
    }

    private fun loadData() {
        if (!NetworkUtil.isNetworkConnected(this)) {
            blockingAlert(getString(R.string.error_no_internet_connection)) {
                loadData()
            }
            return
        }

        viewModel.loadData()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (BuildConfig.DEBUG) {
            menuInflater.inflate(R.menu.menu_upgrade, menu)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPurchasesUpdated(purchases: List<InAppPurchase>) {
        super.onPurchasesUpdated(purchases)
        updatePurchaseButtons()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.actionConsumePurchase -> showConsumePurchaseDialog()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showConsumePurchaseDialog() {
        if (!BuildConfig.DEBUG) {
            return
        }

        val purchases = purchaseStore.purchases.value
        if (purchases == null) {
            Toast.makeText(this, "Nothing to consume", Toast.LENGTH_SHORT).show()
            return
        }

        val completedPurchases =
            purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isSubscription }
        if (completedPurchases.isEmpty()) {
            Toast.makeText(this, "Nothing to consume", Toast.LENGTH_SHORT).show()
            return
        }

        val listItems =
            completedPurchases.map { "${it.products.joinToString(", ")} (${it.orderId})" }
        AlertDialog.Builder(this)
            .setAdapter(
                ArrayAdapter(
                    this,
                    android.R.layout.select_dialog_singlechoice,
                    listItems
                )
            ) { dialog, which ->
                viewModel.consume(completedPurchases[which])
                dialog.dismiss()
            }
            .show()
    }

    private fun handlePurchaseError(result: BillingResult) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            return
        }
        if (result.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            return
        }
        val message = result.debugMessage.ifBlank {
            getString(R.string.activity_upgrade_error_purchase_failed)
        }
        alert(message)
    }

    private fun handleBillingError(result: BillingResult) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            return
        }
        if (result.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            return
        }
        val message = result.debugMessage.ifBlank {
            getString(R.string.activity_upgrade_error_purchase_failed)
        }

        blockingAlert(message) {
            viewModel.loadData()
        }
    }

    private fun onPurchaseStateChanged(state: InAppPurchaseState) {
        if (state is InAppPurchaseState.Error) {
            handlePurchaseError(state.billingResult)
            viewModel.onPurchaseResultDelivered()
            return
        }

        if (state is InAppPurchaseState.Success) {
            val purchases = state.purchases
            val purchasedVerified =
                purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && it.verificationStatus == VerificationStatus.VERIFIED }
            val purchasedProducts = purchasedVerified.map { it.products }.flatten()
            val message = when {
                purchasedProducts.any {
                    arrayOf(
                        AppSkus.PRODUCT_SKU_PLUS_TO_PRO,
                        AppSkus.PRODUCT_SKU_PRO
                    ).contains(it)
                } -> getString(R.string.activity_upgrade_active_plan_pro)

                purchasedProducts.any { arrayOf(AppSkus.SUBSCRIPTION_PRO).contains(it) } -> getString(
                    R.string.activity_upgrade_active_plan_pro
                )

                purchasedProducts.any { arrayOf(AppSkus.PRODUCT_SKU_PLUS).contains(it) } -> getString(
                    R.string.activity_upgrade_active_plan_plus
                )

                else -> ""
            }
            if (message.isNotEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
            val purchasesFailed =
                purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && it.verificationStatus == VerificationStatus.VERIFICATION_FAILED }
            if (purchasesFailed.any()) {
                alert(getString(R.string.activity_upgrade_error_purchase_verification_failed))
            }
            viewModel.onPurchaseResultDelivered()
        }
    }

    private fun checkForPromotionalTexts() {
        if (isPro) {
            return
        }

        lifecycleScope.launch {
            kotlin.runCatching {
                val text = PromotionChecker().checkForPromotion()
                withContext(Dispatchers.Main) {
                    if (text.isNotEmpty()) {
                        showPromo(text)
                    }
                }
            }.onFailure {
                Timber.e(it, "Cannot load promo text")
            }
        }
    }

    private fun showPromo(message: String) {
        promoTextView.text = message
        promoTextView.visibility = if (isPro) View.GONE else View.VISIBLE
    }

    private fun findPlanOptions(planCard: CardView): List<PlanOptionView> {
        val options: MutableList<PlanOptionView> = ArrayList()
        val layout = planCard.getChildAt(0) as ViewGroup
        for (i in 0 until layout.childCount) {
            val child = layout.getChildAt(i)
            if (child is PlanOptionView) {
                options.add(child)
            }
        }
        return options
    }

    private fun onPlusUpgradeClicked() {
        if (viewModel.screenState.value !is UpgradeScreenState.Success) {
            return
        }
        if (isPlus) {
            return
        }
        if (purchaseStore.hasPendingPurchases || viewModel.purchaseState.value is InAppPurchaseState.Processing) {
            alert(getString(R.string.activity_upgrade_has_pending_purchase))
            return
        }
        viewModel.purchase(this, AppSkus.PRODUCT_SKU_PLUS)
    }

    private fun onProUpgradeClicked() {
        if (viewModel.screenState.value !is UpgradeScreenState.Success) {
            return
        }
        if (isPro) {
            return
        }
        if (purchaseStore.hasPendingPurchases || viewModel.purchaseState.value is InAppPurchaseState.Processing) {
            alert(getString(R.string.activity_upgrade_has_pending_purchase))
            return
        }

        val plusState = purchaseStore.plusState()
        if (plusState == Purchase.PurchaseState.UNSPECIFIED_STATE) {
            viewModel.purchase(this, AppSkus.PRODUCT_SKU_PRO)
        } else if (plusState == Purchase.PurchaseState.PURCHASED) {
            viewModel.purchase(this, AppSkus.PRODUCT_SKU_PLUS_TO_PRO)
        }
    }

    private fun onProSubscriptionUpgradeClicked() {
        if (viewModel.screenState.value !is UpgradeScreenState.Success) {
            return
        }
        if (isPro) {
            return
        }
        if (purchaseStore.hasPendingPurchases || viewModel.purchaseState.value is InAppPurchaseState.Processing) {
            alert(getString(R.string.activity_upgrade_has_pending_purchase))
            return
        }
        viewModel.purchase(this, AppSkus.SUBSCRIPTION_PRO)
    }

    private fun plusVersionMessage(): String {
        val state = viewModel.screenState.value
        val products = if (state is UpgradeScreenState.Success) {
            state.products
        } else {
            return getString(R.string.activity_upgrade_price_loading)
        }

        val plusState = purchaseStore.plusState()
        if (plusState == Purchase.PurchaseState.PURCHASED) {
            return getString(R.string.activity_upgrade_active_plan_plus)
        } else if (plusState == Purchase.PurchaseState.PENDING) {
            return getString(R.string.activity_upgrade_awaiting_payment)
        }

        val plusProduct = products.firstOrNull { it.productId == AppSkus.PRODUCT_SKU_PLUS }
        return if (plusProduct != null) {
            getString(
                R.string.activity_upgrade_message_one_time_purchase_format,
                plusProduct.oneTimePurchaseOfferDetails?.formattedPrice
            )
        } else {
            getString(R.string.activity_upgrade_product_not_found)
        }
    }

    private fun proVersionMessage(): String {
        val state = viewModel.screenState.value
        val products = if (state is UpgradeScreenState.Success) {
            state.products
        } else {
            return getString(R.string.activity_upgrade_price_loading)
        }

        val proState = purchaseStore.proState()
        if (proState == Purchase.PurchaseState.PURCHASED) {
            return ""
        } else if (proState == Purchase.PurchaseState.PENDING) {
            return getString(R.string.activity_upgrade_awaiting_payment)
        }

        val proProduct = if (isPlus) {
            products.firstOrNull { it.productId == AppSkus.PRODUCT_SKU_PLUS_TO_PRO }
        } else {
            products.firstOrNull { it.productId == AppSkus.PRODUCT_SKU_PRO }
        }

        return if (proProduct != null) {
            getString(
                R.string.activity_upgrade_message_one_time_purchase_format,
                proProduct.oneTimePurchaseOfferDetails?.formattedPrice
            )
        } else {
            getString(R.string.activity_upgrade_product_not_found)
        }
    }

    private fun updatePurchaseButtons() {
        plusButton.visibility = View.VISIBLE
        plusPurchaseUpgradeText.visibility = View.VISIBLE
        proPurchaseUpgradeText.text = getString(R.string.activity_upgrade_action_upgrade_to_plus)
        plusPurchasePriceText.visibility = View.VISIBLE
        proButton.visibility = View.VISIBLE
        proPurchaseUpgradeText.visibility = View.VISIBLE
        proPurchaseUpgradeText.text = getString(R.string.activity_upgrade_action_upgrade_to_pro)
        proPurchasePriceText.visibility = View.VISIBLE
        proSubscriptionButton.visibility = View.VISIBLE
        proSubscriptionUpgradeText.visibility = View.VISIBLE
        proSubscriptionUpgradeText.text =
            getString(R.string.activity_upgrade_action_subscribe_to_pro)
        proSubscriptionPriceText.visibility = View.VISIBLE

        plusPurchasePriceText.text = plusVersionMessage()
        proPurchasePriceText.text = proVersionMessage()

        val screenState = viewModel.screenState.value
        if (screenState is UpgradeScreenState.Success && !screenState.supportsSubscriptions) {
            proSubscriptionButton.visibility = View.GONE
        }

        if (isPro) {
            plusPurchaseUpgradeText.text = getString(R.string.activity_upgrade_active_plan_plus)
            proPurchaseUpgradeText.text = getString(R.string.activity_upgrade_active_plan_pro)
            plusButton.visibility = View.GONE
            proPurchasePriceText.visibility = View.GONE
            proSubscriptionButton.visibility = View.GONE
            promoTextView.visibility = View.GONE
            plusOptions.forEach { it.setOptionEnabled(true) }
            proOptions.forEach { it.setOptionEnabled(true) }
            return
        }

        if (isPlus) {
            plusPurchaseUpgradeText.text = getString(R.string.activity_upgrade_active_plan_plus)
            plusPurchasePriceText.visibility = View.GONE
            plusOptions.forEach { it.setOptionEnabled(true) }
            proOptions.forEach { it.setOptionEnabled(false) }
            setProSubscriptionButtonText()
            return
        }

        plusOptions.forEach { it.setOptionEnabled(false) }
        proOptions.forEach { it.setOptionEnabled(false) }

        setProSubscriptionButtonText()
    }

    private fun setProSubscriptionButtonText() {
        val state = viewModel.screenState.value
        val products = if (state is UpgradeScreenState.Success) {
            state.products
        } else {
            emptyList()
        }

        val skuDetails = products.firstOrNull { it.productId == AppSkus.SUBSCRIPTION_PRO }
        if (skuDetails == null) {
            proSubscriptionPriceText.text = getString(R.string.activity_upgrade_price_loading)
            return
        }

        val offerDetails = skuDetails.subscriptionOfferDetails?.get(0)
        val pricingDetails = offerDetails?.pricingPhases?.pricingPhaseList?.get(0)
        val period = when (pricingDetails?.billingPeriod) {
            "P1W" -> getString(R.string.billing_period_one_week)
            "P1M" -> getString(R.string.billing_period_one_month)
            "P3M" -> getString(R.string.billing_period_three_months)
            "P6M" -> getString(R.string.billing_period_six_months)
            "P1Y" -> getString(R.string.billing_period_one_year)
            else -> ""
        }
        proSubscriptionPriceText.text =
            String.format("%s %s", pricingDetails?.formattedPrice, period)
    }

    private fun blockingAlert(message: String, onOk: Runnable? = null) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton(getString(R.string.action_ok)) { dialog, _ ->
                dialog.dismiss()
                onOk?.run()
            }
            .setNegativeButton(getString(R.string.action_cancel)) { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .setCancelable(false)
            .create()
            .show()
    }

    private fun alert(message: String, onOk: Runnable? = null) {
        Timber.d("Showing alert dialog: %s", message)
        AlertDialog.Builder(this)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(getString(R.string.action_ok)) { dialog, _ ->
                dialog.dismiss()
                onOk?.run()
            }
            .create()
            .show()
    }
}