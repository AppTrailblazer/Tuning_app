package com.willeypianotuning.toneanalyzer.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast

object IntentUtils {
    fun openPlayStore(context: Context, packageName: String): Intent {
        return openPlayStore(context, packageName, true)
    }

    fun openPlayStore(context: Context, packageName: String, openInBrowser: Boolean): Intent {
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
        if (isIntentAvailable(context, marketIntent)) {
            return marketIntent
        }
        return if (openInBrowser) {
            openLink("https://play.google.com/store/apps/details?id=$packageName")
        } else marketIntent
    }

    @JvmStatic
    fun openPlayStoreSubscriptions(packageName: String): Intent {
        return Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/account/subscriptions?sku=com.willeypianotuning.toneanalyzer.subs.pro&package=$packageName")
        )
    }

    fun openLink(url: String): Intent {
        val uri = if (url.isNotEmpty() && !url.contains("://")) {
            Uri.parse("http://$url")
        } else {
            Uri.parse(url)
        }
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        intent.data = uri
        return intent
    }

    @JvmStatic
    fun openApplicationSettings(context: Context): Intent {
        return Intent()
            .setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .addCategory(Intent.CATEGORY_DEFAULT)
            .setData(Uri.parse("package:" + context.packageName))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            .addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
    }

    @JvmStatic
    fun startActivitySafe(activity: Activity, intent: Intent) {
        kotlin.runCatching {
            activity.startActivity(intent)
        }.onFailure {
            Toast.makeText(activity, "No application found to handle intent", Toast.LENGTH_SHORT)
                .show()
        }
    }

    fun isIntentAvailable(context: Context, intent: Intent): Boolean {
        val packageManager = context.packageManager
        val list: List<*> =
            packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return list.isNotEmpty()
    }
}