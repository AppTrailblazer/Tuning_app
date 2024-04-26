package com.willeypianotuning.toneanalyzer.ui.settings.weights

import android.os.Bundle
import com.willeypianotuning.toneanalyzer.store.db.tuning_styles.TuningStyle
import timber.log.Timber

class TuningStyleHistoryManager(private val originalStyle: TuningStyle) {
    private val history: ArrayList<TuningStyle> = arrayListOf(originalStyle)

    fun onModified(tuningStyle: TuningStyle) {
        synchronized(history) {
            val lastState = history.lastOrNull() ?: originalStyle
            if (lastState == tuningStyle) {
                Timber.d("Supplied tuning style has no changes")
                return
            }

            if (history.size == MAX_HISTORY_SIZE) {
                history.removeAt(0)
            }
            Timber.d("Adding new entry to history: $tuningStyle")
            history.add(tuningStyle)
        }
    }

    fun canRestore(): Boolean {
        synchronized(history) {
            return history.size > 1
        }
    }

    fun restore(): TuningStyle? {
        var result: TuningStyle?
        synchronized(history) {
            if (history.size <= 1) {
                return originalStyle
            }
            history.removeAt(history.lastIndex)
            result = history.last()
        }
        return result
    }

    fun restoreInstanceState(state: Bundle) {
        history.clear()
        history.addAll(state.getParcelableArrayList(KEY_HISTORY) ?: emptyList())
    }

    fun saveInstanceState(state: Bundle) {
        state.putParcelableArrayList(KEY_HISTORY, history)
    }

    fun clear() {
        synchronized(history) {
            history.clear()
        }
    }

    companion object {
        private const val MAX_HISTORY_SIZE = 30
        private const val KEY_HISTORY = "tuningStyleHistory"
    }
}