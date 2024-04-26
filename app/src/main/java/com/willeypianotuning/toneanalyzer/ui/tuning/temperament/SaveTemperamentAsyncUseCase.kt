package com.willeypianotuning.toneanalyzer.ui.tuning.temperament

import android.content.Context
import android.widget.Toast
import com.willeypianotuning.toneanalyzer.R
import com.willeypianotuning.toneanalyzer.di.IoDispatcher
import com.willeypianotuning.toneanalyzer.di.MainDispatcher
import com.willeypianotuning.toneanalyzer.store.TemperamentDataStore
import com.willeypianotuning.toneanalyzer.store.db.temperaments.Temperament
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class SaveTemperamentAsyncUseCase
@Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineContext,
    @MainDispatcher private val mainDispatcher: CoroutineContext,
    private val globalScope: CoroutineScope,
    private val temperamentDataStore: TemperamentDataStore,
) {
    operator fun invoke(temperament: Temperament) {
        globalScope.launch(ioDispatcher) {
            kotlin.runCatching {
                temperamentDataStore.addTemperament(temperament)
                Timber.d("Temperament is saved")
                withContext(mainDispatcher) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.message_temperament_saved),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }.onFailure {
                Timber.e(it, "Cannot save temperaments")
            }
        }
    }
}