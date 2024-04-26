package com.willeypianotuning.toneanalyzer.ui.commons

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.willeypianotuning.toneanalyzer.AppSettings
import com.willeypianotuning.toneanalyzer.R
import com.willeypianotuning.toneanalyzer.billing.InAppPurchase
import com.willeypianotuning.toneanalyzer.billing.PurchaseStore
import kotlinx.coroutines.launch

abstract class BaseActivity : AppCompatActivity() {

    private val localBroadcastManager: LocalBroadcastManager by lazy {
        LocalBroadcastManager.getInstance(
            this
        )
    }
    private val nightModeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            applyNightMode()
        }
    }

    protected val appSettings: AppSettings by lazy { AppSettings(this) }

    val purchaseStore: PurchaseStore by lazy { PurchaseStore.getInstance(this) }

    inline val isPro: Boolean get() = purchaseStore.isPro
    inline val isPlus: Boolean get() = purchaseStore.isPlus

    private var toast: Toast? = null

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        localBroadcastManager.registerReceiver(
            nightModeReceiver,
            IntentFilter(AppSettings.KEY_APPEARANCE)
        )
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        lifecycleScope.launch {
            purchaseStore.purchases.collect {
                onPurchasesUpdated(it)
            }
        }
    }

    open fun onPurchasesUpdated(purchases: List<InAppPurchase>) {

    }

    fun setupToolbar(upButton: Boolean = true) {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(upButton)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressedDispatcher.onBackPressed()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun applyNightMode() {
        AppCompatDelegate.setDefaultNightMode(appSettings.appearance)
        delegate.localNightMode = appSettings.appearance
    }

    protected fun notifyNightModeChange() {
        localBroadcastManager.sendBroadcast(Intent(AppSettings.KEY_APPEARANCE))
    }

    fun shouldUseNightMode(): Boolean {
        return when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> true
            Configuration.UI_MODE_NIGHT_NO -> false
            Configuration.UI_MODE_NIGHT_UNDEFINED -> false
            else -> false
        }
    }

    fun showToast(message: String, longToast: Boolean) {
        // we cancel previous toast to avoid issue of queued toasts
        toast?.cancel()
        toast =
            Toast.makeText(this, message, if (longToast) Toast.LENGTH_LONG else Toast.LENGTH_SHORT)
                .also {
                    it.show()
                }
    }

    @CallSuper
    override fun onDestroy() {
        super.onDestroy()
        localBroadcastManager.unregisterReceiver(nightModeReceiver)
    }
}
