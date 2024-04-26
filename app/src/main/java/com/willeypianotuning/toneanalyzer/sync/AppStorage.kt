package com.willeypianotuning.toneanalyzer.sync

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

class AppStorage @Inject constructor(@ApplicationContext private val context: Context) {
    fun getCacheDir(): File {
        return context.cacheDir
    }
}