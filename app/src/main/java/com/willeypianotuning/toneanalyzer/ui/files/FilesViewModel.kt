package com.willeypianotuning.toneanalyzer.ui.files

import androidx.lifecycle.*
import com.willeypianotuning.toneanalyzer.AppSettings
import com.willeypianotuning.toneanalyzer.store.PianoTuningDataStore
import com.willeypianotuning.toneanalyzer.store.db.tunings.PianoTuningInfo
import com.willeypianotuning.toneanalyzer.ui.files.state.DeleteTuningState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import javax.inject.Inject

@HiltViewModel
class FilesViewModel @Inject constructor(
    private val tuningDataStore: PianoTuningDataStore,
    private val appSettings: AppSettings
) : ViewModel() {
    private val sortOrderData = MutableLiveData(appSettings.filesSortOrder)
    val sortOrder: LiveData<Int>
        get() = sortOrderData
    private val _sortedTuningsListData = MediatorLiveData<List<PianoTuningInfo>>()
    val tuningsList: LiveData<List<PianoTuningInfo>>
        get() {
            return _sortedTuningsListData
        }

    private val _deleteFilesState = MutableLiveData<DeleteTuningState>(DeleteTuningState.NotSet)

    @Suppress("unused")
    val deleteFilesState: LiveData<DeleteTuningState>
        get() = _deleteFilesState

    init {
        val tuningsListData =
            tuningDataStore.getTuningsList().asLiveData(viewModelScope.coroutineContext)
        _sortedTuningsListData.addSource(tuningsListData) {
            sortFiles(
                tuningsListData,
                sortOrderData,
                _sortedTuningsListData
            )
        }
        _sortedTuningsListData.addSource(sortOrderData) {
            sortFiles(
                tuningsListData,
                sortOrderData,
                _sortedTuningsListData
            )
        }
    }

    private fun sortFiles(
        tuningsData: LiveData<List<PianoTuningInfo>>,
        sortOrderData: LiveData<Int>,
        result: MediatorLiveData<List<PianoTuningInfo>>
    ) {
        val tunings = tuningsData.value
        val sortOrder = sortOrderData.value
        if (tunings != null && sortOrder != null) {
            Collections.sort(tunings, FileSortOrder.getComparator(sortOrder))
            result.value = tunings
        }
    }

    fun changeSortOrder(sortOrder: Int, makeDefault: Boolean) {
        this.sortOrderData.postValue(sortOrder)
        if (makeDefault) {
            appSettings.filesSortOrder = sortOrder
        }
    }

    fun renameTuning(tuning: PianoTuningInfo) {
        viewModelScope.launch {
            kotlin.runCatching {
                tuningDataStore.updateTuningName(tuning.id, tuning.name)
            }.onFailure {
                Timber.e(it, "Cannot rename file")
            }
        }
    }

    fun copyTuning(tuning: PianoTuningInfo) {
        viewModelScope.launch {
            kotlin.runCatching {
                tuningDataStore.copyTuning(tuning.id)
            }.onFailure {
                Timber.e(it, "Cannot copy file")
            }
        }
    }

    fun deleteTuning(tuning: PianoTuningInfo) {
        deleteTunings(listOf(tuning))
    }

    fun deleteTunings(tunings: List<PianoTuningInfo>) {
        viewModelScope.launch {
            kotlin.runCatching {
                _deleteFilesState.postValue(DeleteTuningState.Loading)
                tuningDataStore.deleteManyByIds(tunings.map { it.id }.toTypedArray())
                _deleteFilesState.postValue(DeleteTuningState.Success)
            }.onFailure {
                Timber.e(it, "Cannot delete tunings")
                _deleteFilesState.postValue(DeleteTuningState.Error(it))
            }
        }
    }

    @Suppress("unused")
    fun onDeleteTuningsResultProcessed() {
        _deleteFilesState.postValue(DeleteTuningState.NotSet)
    }
}