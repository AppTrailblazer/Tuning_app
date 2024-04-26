package com.willeypianotuning.toneanalyzer.ui.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.willeypianotuning.toneanalyzer.AppSettings
import com.willeypianotuning.toneanalyzer.BuildConfig
import com.willeypianotuning.toneanalyzer.R
import com.willeypianotuning.toneanalyzer.audio.AudioRecorder
import com.willeypianotuning.toneanalyzer.audio.enums.InfoBoxText
import com.willeypianotuning.toneanalyzer.audio.note_names.NoteNames
import com.willeypianotuning.toneanalyzer.billing.PurchaseStore
import com.willeypianotuning.toneanalyzer.store.db.tunings.PianoTuning
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class InfoBoxUpdatesHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioRecorder: AudioRecorder,
    private val purchaseStore: PurchaseStore,
    private val appSettings: AppSettings
) {

    private val timeChangedReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Timber.d("Updating time")
            updateNow()
        }
    }

    private val _infoBoxTextLiveData = MutableLiveData<CharSequence>("")
    val infoBoxTextLiveData: LiveData<CharSequence> get() = _infoBoxTextLiveData

    private var registered: Boolean = false

    fun start() {
        if (registered) {
            return
        }

        val infoBoxText = appSettings.infoBoxText
        if (infoBoxText and InfoBoxText.CLOCK != 0) {
            val filter = IntentFilter()
            filter.addAction(Intent.ACTION_TIME_TICK)
            filter.addAction(Intent.ACTION_TIME_CHANGED)
            filter.addAction(Intent.ACTION_TIMEZONE_CHANGED)
            context.registerReceiver(timeChangedReceiver, filter)
            registered = true
        }
    }

    fun updateNow() {
        val tuning = audioRecorder.tuning ?: return
        _infoBoxTextLiveData.postValue(formatInfo(tuning))
    }

    fun stop() {
        if (!registered) {
            return
        }

        context.unregisterReceiver(timeChangedReceiver)
        registered = false
    }

    private fun SpannableStringBuilder.appendSpaceIfNotEmpty(): SpannableStringBuilder {
        if (this.isNotEmpty()) {
            append(" ")
        }
        return this
    }

    private fun formatInfo(tuning: PianoTuning): Spannable {
        val noteNamingConvention =
            NoteNames.getNamingConvention(context, appSettings.noteNames)

        val annotatedStringBuilder = SpannableStringBuilder()

        val infoBoxText: Int = appSettings.infoBoxText
        if (infoBoxText and InfoBoxText.MAKE != 0) {
            val make: String = tuning.make
            if (make.isNotEmpty()) {
                annotatedStringBuilder.appendSpaceIfNotEmpty().append(make)
            }
        }
        if (infoBoxText and InfoBoxText.MODEL != 0) {
            val model: String = tuning.model
            if (model.isNotEmpty()) {
                annotatedStringBuilder.appendSpaceIfNotEmpty().append(model)
            }
        }
        if (infoBoxText and InfoBoxText.PITCH_OFFSET != 0) {
            val pitchNote = String.format("%s", noteNamingConvention.noteName(0))
            val pitchValue = String.format(Locale.getDefault(), "%.1f", tuning.pitch)
            val pitchSpan = SpannableString(pitchValue)
            if (appSettings.globalPitchOffset != tuning.pitch) {
                pitchSpan.setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(context, R.color.warning)),
                    0,
                    pitchSpan.length,
                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE
                )
            }
            annotatedStringBuilder.appendSpaceIfNotEmpty()
                .append(pitchNote)
                .append("=")
                .append(pitchSpan)
        }
        if (infoBoxText and InfoBoxText.CLOCK != 0) {
            val timeFormatter = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT)
            val time = timeFormatter.format(Date(System.currentTimeMillis()))
            annotatedStringBuilder.appendSpaceIfNotEmpty().append(time)
        }
        if (!(purchaseStore.isPlus || purchaseStore.isPro)) {
            annotatedStringBuilder.appendSpaceIfNotEmpty().append(context.getString(R.string.app_plan_lock_free))
        }
        if (BuildConfig.DEBUG) {
            annotatedStringBuilder.appendSpaceIfNotEmpty().append("DEBUG")
        }
        return annotatedStringBuilder
    }
}