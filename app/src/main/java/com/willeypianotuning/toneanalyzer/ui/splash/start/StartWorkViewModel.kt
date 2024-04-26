package com.willeypianotuning.toneanalyzer.ui.splash.start

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.willeypianotuning.toneanalyzer.AppSettings
import com.willeypianotuning.toneanalyzer.ToneDetectorWrapper
import com.willeypianotuning.toneanalyzer.audio.AudioRecorder
import com.willeypianotuning.toneanalyzer.audio.PitchRaiseOptions
import com.willeypianotuning.toneanalyzer.billing.PurchaseStore
import com.willeypianotuning.toneanalyzer.store.PianoTuningDataStore
import com.willeypianotuning.toneanalyzer.store.db.tunings.PianoTuning
import com.willeypianotuning.toneanalyzer.ui.main.tasks.PianoTuningInitializer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class StartWorkViewModel @Inject constructor(
    private val appSettings: AppSettings,
    private val tuningInitializer: PianoTuningInitializer,
    private val audioRecorder: AudioRecorder,
    private val toneAnalyzer: ToneDetectorWrapper,
    private val tuningDataStore: PianoTuningDataStore,
    private val purchaseStore: PurchaseStore
) : ViewModel() {
    private val _startWorkState = MutableLiveData<StartWorkScreenState>(StartWorkScreenState.NotSet)
    val startWorkState: LiveData<StartWorkScreenState> get() = _startWorkState

    private val _resumeWorkState = MutableLiveData<ResumeWorkState>(ResumeWorkState.NotSet)
    val resumeWorkState: LiveData<ResumeWorkState> get() = _resumeWorkState

    private val _selectedTuning = MutableLiveData<StartWorkState>(StartWorkState.NotSet)
    val selectedTuning: LiveData<StartWorkState> get() = _selectedTuning

    val isPro: Boolean get() = purchaseStore.isPro

    init {
        if (appSettings.currentTuningId != null) {
            viewModelScope.launch {
                kotlin.runCatching {
                    _startWorkState.postValue(StartWorkScreenState.Loading)
                    val tuning = tuningDataStore.getTuning(appSettings.currentTuningId!!)
                    _startWorkState.postValue(
                        StartWorkScreenState.Success(
                            tuning, purchaseStore.isPro
                        )
                    )
                }.onFailure {
                    Timber.e(it, "Failed to fetch last tuning")
                    _startWorkState.postValue(
                        StartWorkScreenState.Success(
                            null, purchaseStore.isPro
                        )
                    )
                }
            }
        } else {
            _startWorkState.postValue(StartWorkScreenState.Success(null, purchaseStore.isPro))
        }
        viewModelScope.launch {
            purchaseStore.purchases.collect {
                val startWorkState = _startWorkState.value
                if (startWorkState !is StartWorkScreenState.Success) {
                    return@collect
                }
                if (startWorkState.isPro != purchaseStore.isPro) {
                    _startWorkState.postValue(startWorkState.copy(isPro = purchaseStore.isPro))
                }
            }
        }
    }

    fun createNewTuning(): PianoTuning {
        return tuningInitializer.initializeNewTuning()
    }

    fun onPianoTuningSelected(tuning: PianoTuning) {
        _selectedTuning.postValue(StartWorkState.TuningSelected(tuning))
    }

    fun removeSelectedTuning() {
        _selectedTuning.postValue(StartWorkState.NotSet)
    }

    fun loadTuning(tuningId: String) {
        if (resumeWorkState.value != ResumeWorkState.NotSet) {
            Timber.w("Can't start pitch raise. Resume in progress")
            return
        }

        viewModelScope.launch {
            kotlin.runCatching {
                _resumeWorkState.postValue(ResumeWorkState.Loading)
                val tuning = tuningDataStore.getTuning(tuningId)
                appSettings.currentTuningId = tuning.id
                audioRecorder.tuning = tuning
                _resumeWorkState.postValue(ResumeWorkState.Success(tuning, null))
            }.onFailure {
                _resumeWorkState.postValue(ResumeWorkState.Error(it))
            }
        }
    }

    fun tunePianoWith(tuning: PianoTuning, pitchRaiseOptions: PitchRaiseOptions? = null) {
        if (resumeWorkState.value != ResumeWorkState.NotSet) {
            Timber.w("Can't start pitch raise. Resume in progress")
            return
        }

        viewModelScope.launch {
            kotlin.runCatching {
                _resumeWorkState.postValue(ResumeWorkState.Loading)
                val savedTuning = tuningDataStore.addTuning(tuning)
                appSettings.currentTuningId = savedTuning.id
                audioRecorder.tuning = savedTuning
                if (pitchRaiseOptions != null) {
                    synchronized(toneAnalyzer.pitchRaiseModeLock) {
                        toneAnalyzer.startPitchRaiseMeasurement(pitchRaiseOptions)
                    }
                }
                _resumeWorkState.postValue(ResumeWorkState.Success(savedTuning, pitchRaiseOptions))
            }.onFailure {
                _resumeWorkState.postValue(ResumeWorkState.Error(it))
            }
        }
    }

    fun onResultDelivered() {
        _resumeWorkState.postValue(ResumeWorkState.NotSet)
    }

}