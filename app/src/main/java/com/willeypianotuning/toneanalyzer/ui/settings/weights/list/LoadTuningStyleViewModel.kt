package com.willeypianotuning.toneanalyzer.ui.settings.weights.list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.willeypianotuning.toneanalyzer.store.TuningStyleDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LoadTuningStyleViewModel @Inject constructor(
    private val tuningStyleDs: TuningStyleDataStore
) : ViewModel() {

    private val _screenState = MutableLiveData<LoadTuningStyleScreenState>(null)
    val screenState: LiveData<LoadTuningStyleScreenState> get() = _screenState

    fun load() {
        viewModelScope.launch {
            kotlin.runCatching {
                _screenState.postValue(LoadTuningStyleScreenState.Loading)
                val styles = withContext(Dispatchers.IO) {
                    tuningStyleDs.allStyles()
                }
                _screenState.postValue(LoadTuningStyleScreenState.Success(styles))
            }.onFailure {
                Timber.e(it, "Cannot load tuning styles")
                _screenState.postValue(LoadTuningStyleScreenState.Error(it))
            }
        }
    }

}