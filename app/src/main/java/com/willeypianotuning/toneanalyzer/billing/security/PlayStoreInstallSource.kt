package com.willeypianotuning.toneanalyzer.billing.security

import android.content.Context

class PlayStoreInstallSource : BaseInstallSource() {
    override fun checkInstalledWith(context: Context): Boolean {
        // reversed "com.android.vending"
        val packageName = "gnidnev.diordna.moc"
        return getInstallSource(context) == packageName.reversed()
    }
}