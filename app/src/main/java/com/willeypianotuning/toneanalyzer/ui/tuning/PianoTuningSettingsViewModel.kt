package com.willeypianotuning.toneanalyzer.ui.tuning

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.willeypianotuning.toneanalyzer.store.PianoTuningDataStore
import com.willeypianotuning.toneanalyzer.store.db.tunings.PianoTuning
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PianoTuningSettingsViewModel @Inject constructor(
    private val pianoTuningDataStore: PianoTuningDataStore,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _tuning = MutableStateFlow<PianoTuning>(
        requireNotNull(savedStateHandle[TuningSettingsActivity.EXTRA_TUNING_SETTINGS]) {
            "Tuning is not available"
        }
    )
    val tuning: StateFlow<PianoTuning> get() = _tuning

    fun updateTuning(tuning: PianoTuning) {
        _tuning.value = tuning
        savedStateHandle[TuningSettingsActivity.EXTRA_TUNING_SETTINGS] = tuning
    }

    fun saveTuning() {
        val tuning = tuning.value

        viewModelScope.launch {
            kotlin.runCatching {
                pianoTuningDataStore.updateTuning(tuning)
                Timber.d("Tuning data updated (${tuning.id}, ${tuning.make}, ${tuning.model}, ${tuning.name}")
            }.onFailure {
                Timber.e(it, "Cannot update tuning")
            }
        }
    }
}