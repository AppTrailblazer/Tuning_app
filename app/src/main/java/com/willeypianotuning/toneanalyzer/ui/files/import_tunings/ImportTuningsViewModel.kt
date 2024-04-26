package com.willeypianotuning.toneanalyzer.ui.files.import_tunings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.willeypianotuning.toneanalyzer.sync.PianoMeterFilesImporter
import com.willeypianotuning.toneanalyzer.sync.RestoreStrategy
import com.willeypianotuning.toneanalyzer.ui.files.state.ImportTuningState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ImportTuningsViewModel @Inject constructor(
    private val pianoTuningFilesImporter: PianoMeterFilesImporter
) : ViewModel() {
    private val _importFilesState = MutableLiveData<ImportTuningState>(ImportTuningState.NotSet)
    val importFilesState: LiveData<ImportTuningState>
        get() = _importFilesState

    private suspend fun isZip(location: ImportLocation): Boolean {
        var isZip = false
        location.withInputStream {
            withContext(Dispatchers.IO) {
                val header = ByteArray(4)
                it.read(header)
                isZip = header.contentEquals(byteArrayOf(0x50, 0x4b, 0x03, 0x04))
            }
        }
        return isZip
    }

    private suspend fun importEtf(location: ImportLocation, strategy: RestoreStrategy) {
        Timber.i("Importing ${location.name()} as ETF")
        location.withInputStream {
            pianoTuningFilesImporter.importEtf(it, strategy)
        }
    }

    private suspend fun importEtfz(location: ImportLocation, strategy: RestoreStrategy) {
        Timber.i("Importing ${location.name()} as ETFZ")
        location.withInputStream {
            pianoTuningFilesImporter.importEtfz(it, strategy)
        }
    }

    fun importTunings(importLocation: ImportLocation, strategy: RestoreStrategy) {
        viewModelScope.launch {
            _importFilesState.postValue(ImportTuningState.Loading)
            kotlin.runCatching {
                withContext(Dispatchers.IO) {
                    val name = importLocation.name()
                    when {
                        name.endsWith(".etf") -> {
                            kotlin.runCatching {
                                importEtf(importLocation, strategy)
                            }.onFailure {
                                if (isZip(importLocation)) {
                                    importEtfz(importLocation, strategy)
                                } else {
                                    Timber.e(it, "Failed to import ETF")
                                }
                            }
                        }
                        name.endsWith(".etfz") -> {
                            importEtfz(importLocation, strategy)
                        }
                    }
                    _importFilesState.postValue(ImportTuningState.Success)
                }
            }.onFailure { error ->
                Timber.e(error, "Cannot import tuning files")
                _importFilesState.postValue(ImportTuningState.Error(error))
            }
        }
    }

    fun onImportTuningsResultProcessed() {
        _importFilesState.postValue(ImportTuningState.NotSet)
    }
}