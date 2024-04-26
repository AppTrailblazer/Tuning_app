package com.willeypianotuning.toneanalyzer.billing.security

import android.content.Context

abstract class BaseInstallSource : InstallSource {

    fun getInstallSource(context: Context): String? {
        return context.packageManager.getInstallerPackageName(context.packageName)
    }

}