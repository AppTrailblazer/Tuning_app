package com.willeypianotuning.toneanalyzer.extensions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.karumi.dexter.Dexter
import com.willeypianotuning.toneanalyzer.ui.commons.DialogOnDeniedAudioPermissionListener

fun Context.hasPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(
        this, permission
    ) == PackageManager.PERMISSION_GRANTED
}

fun Context.hasAudioPermission(): Boolean {
    return hasPermission(Manifest.permission.RECORD_AUDIO)
}

fun Context.hasNotificationsPermission(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return true
    }
    return hasPermission(Manifest.permission.POST_NOTIFICATIONS)
}

class PermissionCheckBuilder(
    private val activity: Activity, private val permission: String
) {

    private var onGranted: Runnable = Runnable { }
    private var onDenied: Runnable = Runnable { }

    fun onGranted(action: Runnable) = apply { onGranted = action }
    fun onDenied(action: Runnable) = apply { onDenied = action }

    fun check() {
        if (activity.hasPermission(permission)) {
            onGranted.run()
            return
        }

        Dexter.withContext(activity).withPermission(permission).withListener(
            DialogOnDeniedAudioPermissionListener(activity, onGranted, onDenied)
        ).check()
    }
}

fun Activity.withAudioPermission(): PermissionCheckBuilder {
    return PermissionCheckBuilder(this, Manifest.permission.RECORD_AUDIO)
}

fun Activity.runWithAudioPermission(onGranted: Runnable) {
    this.withAudioPermission().onGranted(onGranted).check()
}

fun Activity.runWithNotificationsPermission(onGranted: Runnable) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        onGranted.run()
        return
    }
    PermissionCheckBuilder(this, Manifest.permission.POST_NOTIFICATIONS).onGranted(onGranted)
        .check()
}

fun Fragment.runWithAudioPermission(onGranted: Runnable) {
    requireActivity().withAudioPermission().onGranted(onGranted).check()
}