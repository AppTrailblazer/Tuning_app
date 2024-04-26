package com.willeypianotuning.toneanalyzer.ui.splash

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.willeypianotuning.toneanalyzer.R
import com.willeypianotuning.toneanalyzer.billing.PurchaseRestoreTask
import com.willeypianotuning.toneanalyzer.databinding.ActivitySplashBinding
import com.willeypianotuning.toneanalyzer.ui.splash.start.StartWorkFragment
import com.willeypianotuning.toneanalyzer.utils.LocaleUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding

    private val viewModel: SplashViewModel by viewModels()

    @Inject
    lateinit var purchaseRestoreTask: PurchaseRestoreTask

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isTaskRoot) {
            // This snippet restores the app from background
            // if app is running in background and the user clicks the app icon in launcher
            finish()
            return
        }

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.screenState.observe(this, this::onScreenStateChanged)
    }

    override fun onStart() {
        super.onStart()
        if (purchaseRestoreTask.shouldRestore()) {
            purchaseRestoreTask.restore()
        }
    }

    private fun onScreenStateChanged(state: SplashScreenState?) {
        if (state == null) {
            return
        }

        if (state is SplashScreenState.None) {
            viewModel.startMigration()
        }

        if (state is SplashScreenState.Running) {
            binding.currentStatusTextView.text = getString(R.string.message_migration_running)
            binding.progressBar.visibility = View.VISIBLE
            return
        }

        if (state is SplashScreenState.Error) {
            binding.currentStatusTextView.text = getString(R.string.error_migration_failed)
            binding.progressBar.visibility = View.INVISIBLE
            return
        }

        if (state is SplashScreenState.Success) {
            binding.currentStatusTextView.visibility = View.GONE
            binding.progressBar.visibility = View.INVISIBLE
            showActionsMenu()
            return
        }
    }

    private fun showActionsMenu() {
        window.setBackgroundDrawableResource(R.color.launcher_background_color)
        binding.container.visibility = View.VISIBLE
        val fragment = supportFragmentManager.findFragmentById(R.id.container)
        if (fragment == null || fragment !is StartWorkFragment) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, StartWorkFragment())
                .commit()
        }
    }

}