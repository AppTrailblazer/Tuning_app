package com.willeypianotuning.toneanalyzer.billing.security

import android.content.Context

interface InstallSource {
    fun checkInstalledWith(context: Context): Boolean
}