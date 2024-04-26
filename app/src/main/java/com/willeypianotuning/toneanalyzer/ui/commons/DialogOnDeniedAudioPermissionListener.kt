package com.willeypianotuning.toneanalyzer.ui.commons

import android.app.Activity
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import com.karumi.dexter.Dexter
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.single.BasePermissionListener
import com.willeypianotuning.toneanalyzer.R
import com.willeypianotuning.toneanalyzer.utils.IntentUtils.openApplicationSettings
import com.willeypianotuning.toneanalyzer.utils.IntentUtils.startActivitySafe

class DialogOnDeniedAudioPermissionListener(
    private val activity: Activity,
    private val grantedAction: Runnable,
    private val deniedAction: Runnable? = null
) : BasePermissionListener() {

    override fun onPermissionGranted(response: PermissionGrantedResponse) {
        super.onPermissionGranted(response)
        grantedAction.run()
    }

    override fun onPermissionDenied(response: PermissionDeniedResponse) {
        super.onPermissionDenied(response)
        val message = if (response.isPermanentlyDenied) {
            activity.getString(R.string.permission_audio_rationale)
        } else {
            activity.getString(R.string.permission_storage_request_message)
        }
        AlertDialog.Builder(activity)
            .setTitle(R.string.app_name)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(R.string.permission_action_grant) { dialog: DialogInterface, _: Int ->
                if (response.isPermanentlyDenied) {
                    startActivitySafe(activity, openApplicationSettings(activity))
                } else {
                    Dexter.withContext(activity)
                        .withPermission(response.permissionName)
                        .withListener(this)
                        .check()
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.permission_action_cancel) { dialog: DialogInterface, _: Int ->
                deniedAction?.run()
                dialog.dismiss()
            }
            .show()
    }

}