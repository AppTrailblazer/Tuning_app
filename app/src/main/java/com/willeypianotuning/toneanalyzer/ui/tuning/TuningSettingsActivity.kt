package com.willeypianotuning.toneanalyzer.ui.tuning

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.viewModels
import androidx.core.content.PackageManagerCompat
import androidx.core.content.pm.ActivityInfoCompat
import androidx.core.view.isVisible
import com.willeypianotuning.toneanalyzer.R
import com.willeypianotuning.toneanalyzer.databinding.ActivityTuningSettingsBinding
import com.willeypianotuning.toneanalyzer.store.db.tunings.PianoTuning
import com.willeypianotuning.toneanalyzer.ui.commons.BaseActivity
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber


@AndroidEntryPoint
class TuningSettingsActivity : BaseActivity(), PianoTuningSettingsFragment.Callback {

    private val viewModel: PianoTuningSettingsViewModel by viewModels()

    private lateinit var binding: ActivityTuningSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTuningSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        setTitle(R.string.activity_tuning_file_settings_title)

        binding.actionContinue.setOnClickListener {
            val tuning = viewModel.tuning.value
            if (tuning == null) {
                Timber.w("Tuning is not available")
                return@setOnClickListener
            }
            setActivityResult(tuning)
            saveTuning(tuning)
            finish()
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.container, PianoTuningSettingsFragment())
            .commit()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                setActivityResult(viewModel.tuning.value)
                finish()
            }
        })
    }

    override fun onStart() {
        super.onStart()
        configureUi(viewModel.tuning.value)
    }

    private fun configureUi(tuning: PianoTuning?) {
        if (tuning == null) return
        binding.actionContinue.isVisible = tuning.isNewTuning
    }

    private fun setActivityResult(tuning: PianoTuning) {
        val data = Intent().apply {
            putExtra(EXTRA_TUNING_SETTINGS, tuning)
        }
        setResult(Activity.RESULT_OK, data)
    }

    override fun onTuningChanged(tuning: PianoTuning) {
        viewModel.updateTuning(tuning)
        setActivityResult(tuning)
        saveTuning(tuning)
    }

    private fun saveTuning(tuning: PianoTuning) {
        if (tuning.isNewTuning) {
            return
        }

        viewModel.saveTuning()
    }

    companion object {
        const val EXTRA_TUNING_SETTINGS = "tuningSettings"
    }

    class Contract : ActivityResultContract<PianoTuning, PianoTuning?>() {
        override fun createIntent(context: Context, input: PianoTuning): Intent {
            return Intent(context, TuningSettingsActivity::class.java).apply {
                putExtra(EXTRA_TUNING_SETTINGS, input)
            }
        }

        override fun parseResult(resultCode: Int, intent: Intent?): PianoTuning? {
            if (resultCode != Activity.RESULT_OK || intent == null) {
                return null
            }
            return intent.getParcelableExtra(EXTRA_TUNING_SETTINGS)
        }
    }
}
