package com.willeypianotuning.toneanalyzer.ui.files.export_tunings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.willeypianotuning.toneanalyzer.store.PianoTuningDataStore
import com.willeypianotuning.toneanalyzer.store.db.tunings.PianoTuningInfo
import com.willeypianotuning.toneanalyzer.sync.AppStorage
import com.willeypianotuning.toneanalyzer.sync.EtfFileWriter
import com.willeypianotuning.toneanalyzer.sync.EtfzFileWriter
import com.willeypianotuning.toneanalyzer.ui.files.state.ShareTuningState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ExportTuningsViewModel @Inject constructor(
    private val tuningDataStore: PianoTuningDataStore,
    private val appStorage: AppStorage
) : ViewModel() {
    private val _shareFilesState = MutableLiveData<ShareTuningState>(ShareTuningState.NotSet)
    val shareFilesState: LiveData<ShareTuningState>
        get() = _shareFilesState

    private fun generateExportFileName(fo: PianoTuningInfo): String {
        var filename = fo.name + ' ' + fo.make + ' ' + fo.model
        // strip reserved characters in filename
        val reserved = arrayOf(
            "\"",
            "*",
            "/",
            ":",
            "<",
            ">",
            "?",
            "\\",
            "|",
            "+",
            ",",
            ".",
            ";",
            "=",
            "[",
            "]"
        )
        for (r in reserved) {
            filename = filename.replace(r, "")
        }
        // remove extra spaces
        filename = filename.replace("\\s{2,}".toRegex(), " ").trim { it <= ' ' }
        return filename
    }

    /**
     * Returns recommended file name for exported tunings
     */
    fun onTuningsForExportSelected(tunings: List<PianoTuningInfo>): String? {
        if (tunings.isEmpty()) {
            return null
        }

        _shareFilesState.value = ShareTuningState.TuningsSelected(tunings.map { it.id })

        return if (tunings.size > 1) {
            val df: DateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
            "export_" + df.format(Date()) + ".etfz"
        } else {
            generateExportFileName(tunings[0]) + ".etf"
        }
    }

    fun onTuningsForShareSelected(tunings: List<PianoTuningInfo>): File? {
        val fileName = onTuningsForExportSelected(tunings) ?: return null
        return File(appStorage.getCacheDir(), fileName)
    }

    fun onExportSelectedTunings(exportLocation: ExportLocation, share: Boolean) {
        val state = _shareFilesState.value as? ShareTuningState.TuningsSelected ?: return

        viewModelScope.launch {
            kotlin.runCatching {
                _shareFilesState.postValue(ShareTuningState.Loading)
                withContext(Dispatchers.IO) {
                    val tunings = tuningDataStore.getMany(state.tuningIds.toTypedArray())
                    exportLocation.withOutputStream { outputStream ->
                        if (tunings.size == 1) {
                            EtfFileWriter(outputStream).use { fileWriter ->
                                fileWriter.writeTuning(tunings[0])
                            }
                        } else {
                            EtfzFileWriter(outputStream).use { fileWriter ->
                                fileWriter.writeTunings(tunings)
                            }
                        }
                    }
                }
                _shareFilesState.postValue(ShareTuningState.Success(exportLocation, share))
            }.onFailure {
                Timber.e(it, "Cannot share tunings")
                _shareFilesState.postValue(ShareTuningState.Error(it))
            }
        }
    }

    fun onShareTuningsResultProcessed() {
        _shareFilesState.postValue(ShareTuningState.NotSet)
    }
}