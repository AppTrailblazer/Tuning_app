package com.willeypianotuning.toneanalyzer.ui.splash

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.willeypianotuning.toneanalyzer.store.migrations.TuningFilesToDatabaseMigration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val tuningsMigration: TuningFilesToDatabaseMigration,
) : ViewModel() {

    private val _screenState = MutableLiveData<SplashScreenState>(SplashScreenState.None)
    val screenState: LiveData<SplashScreenState> get() = _screenState

    private fun shouldRunMigrations(): Boolean {
        return tuningsMigration.shouldRunMigration()
    }

    fun startMigration() {
        if (!shouldRunMigrations()) {
            _screenState.postValue(SplashScreenState.Success)
            return
        }
        viewModelScope.launch {
            kotlin.runCatching {
                _screenState.postValue(SplashScreenState.Running)
                tuningsMigration.migrate()
                _screenState.postValue(SplashScreenState.Success)
            }.onFailure {
                _screenState.postValue(SplashScreenState.Error(it))
                Timber.e(it, "Cannot perform migration from tuning files to database")
            }
        }
    }

}