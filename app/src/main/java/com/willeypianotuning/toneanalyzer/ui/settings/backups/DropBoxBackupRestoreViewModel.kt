package com.willeypianotuning.toneanalyzer.ui.settings.backups

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.willeypianotuning.toneanalyzer.AppSettings
import com.willeypianotuning.toneanalyzer.store.dropbox.DropboxClientFactory
import com.willeypianotuning.toneanalyzer.sync.RestoreStrategy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class DropBoxBackupRestoreViewModel @Inject constructor(
    private val settings: AppSettings,
    private val loadBackupFilesFromDropbox: LoadBackupFilesFromDropboxUseCase,
    private val backupFilesToDropbox: BackupFilesToDropboxUseCase,
    private val restoreBackupFileFromDropbox: RestoreBackupFileFromDropboxUseCase,
) : ViewModel() {
    private val _screenState = MutableLiveData<DropBoxScreenState>()
    val screenState: LiveData<DropBoxScreenState> get() = _screenState

    private val _backupState = MutableLiveData<BackupToDropBoxState>(BackupToDropBoxState.None)
    val backupState: LiveData<BackupToDropBoxState> get() = _backupState

    private val _restoreState =
        MutableLiveData<RestoreFromDropBoxState>(RestoreFromDropBoxState.None)
    val restoreState: LiveData<RestoreFromDropBoxState> get() = _restoreState

    init {
        loadFiles()
    }

    fun backupNow() {
        viewModelScope.launch {
            kotlin.runCatching {
                _backupState.postValue(BackupToDropBoxState.Loading)
                backupFilesToDropbox()
                val dropboxFiles = loadBackupFilesFromDropbox()
                _screenState.postValue(DropBoxScreenState.Data(dropboxFiles))
            }.onSuccess {
                _backupState.postValue(BackupToDropBoxState.Success)
            }.onFailure {
                Timber.e(it, "Failed to backup files to DropBox")
                _backupState.postValue(BackupToDropBoxState.Error)
            }
        }
    }

    fun loadFiles() {
        if (DropboxClientFactory.getCredentials(settings) == null) {
            _screenState.postValue(DropBoxScreenState.Unauthenticated)
            return
        }

        viewModelScope.launch {
            kotlin.runCatching {
                _screenState.postValue(DropBoxScreenState.Loading)
                val files = loadBackupFilesFromDropbox()
                _screenState.postValue(DropBoxScreenState.Data(files))
            }.onFailure {
                Timber.e(it, "Cannot load files list from DropBox")
                _screenState.postValue(DropBoxScreenState.Error)
            }
        }
    }

    fun resetBackupState() {
        _backupState.postValue(BackupToDropBoxState.None)
    }

    fun resetRestoreState() {
        _restoreState.postValue(RestoreFromDropBoxState.None)
    }

    fun restoreFromBackup(backupFile: String, strategy: RestoreStrategy) {
        viewModelScope.launch {
            kotlin.runCatching {
                _restoreState.postValue(RestoreFromDropBoxState.Loading)
                restoreBackupFileFromDropbox(backupFile, strategy)
                _restoreState.postValue(RestoreFromDropBoxState.Success)
            }.onFailure {
                Timber.e(it, "Failed to restore tunings")
                _restoreState.postValue(RestoreFromDropBoxState.Error)
            }
        }
    }

    sealed interface DropBoxScreenState {
        object Unauthenticated : DropBoxScreenState
        object Error : DropBoxScreenState
        object Loading : DropBoxScreenState
        data class Data(val backupFiles: List<String>) : DropBoxScreenState
    }

    sealed interface BackupToDropBoxState {
        object None : BackupToDropBoxState
        object Error : BackupToDropBoxState
        object Loading : BackupToDropBoxState
        object Success : BackupToDropBoxState
    }

    sealed interface RestoreFromDropBoxState {
        object None : RestoreFromDropBoxState
        object Error : RestoreFromDropBoxState
        object Loading : RestoreFromDropBoxState
        object Success : RestoreFromDropBoxState
    }

}