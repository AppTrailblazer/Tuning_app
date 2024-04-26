package com.willeypianotuning.toneanalyzer.billing.security

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import com.willeypianotuning.toneanalyzer.AppSettings
import com.willeypianotuning.toneanalyzer.R
import com.willeypianotuning.toneanalyzer.ToneDetectorWrapper
import com.willeypianotuning.toneanalyzer.utils.IntentUtils
import com.willeypianotuning.toneanalyzer.utils.OrientationUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AntiTamper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val toneAnalyzer: ToneDetectorWrapper,
    private val appSettings: AppSettings
) {

    var alertDialog: AlertDialog? = null
    var noteSwitchTrigger = false

    /**
     * If larger than 0, the app is hacked
     */
    var appProbablyHacked = Integer.MIN_VALUE

    @Suppress("NOTHING_TO_INLINE")
    fun updateAppHackedState() {
        appProbablyHacked = if (isAppProbablyHacked(context)) 1 else -1
        noteSwitchTrigger = AppSettings(context).noteSwitchTrigger()
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun isAppProbablyHacked(context: Context): Boolean {
        val installedFromPlayStore = PlayStoreInstallSource().checkInstalledWith(context)
        if (installedFromPlayStore) {
            return false
        }

        val packageName = getPackageName()
        val signatures = listOf(
            // debug key, SHA1: 63:D2:AD:CF:B9:44:27:02:8F:75:3D:44:97:10:00:74:6F:AA:F4:6D
            PackageSignature(
                packageName,
                SecurityUtils.Aes.Ecb.decrypt(
                    "qDDPXzihV8ogdobiKTQU7U2pt9zIh3oKFfZjut8cONlQ/kltEdcgX7r4/s41Rb0RA66ZuSL3+JEYo0l7oNA4oA==",
                    createKey(context)
                )
            ),
            // release key, SHA1: 63:D7:C0:C9:71:CF:15:C0:9B:05:C7:DB:62:14:B9:97:DE:5D:2E:89
            PackageSignature(
                packageName,
                SecurityUtils.Aes.Ecb.decrypt(
                    "pgr7fA8YzYakvCfpGEssshrEfR2WU3w/AKS+SBxlei3yqqdPiNNu4ua9NlRKH/RxQAW/rbqaU7LDmZVcu0lVvQ==",
                    createKey(context)
                )
            )
        )
        val signaturesValid = PackageSignatureChecker(context).isAnyOfSignaturesValid(signatures)
        return !signaturesValid
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun getPackageName(): String {
        val validPackageId = "oouepynneor.wlciin.lntztanigymalea"
        var indices = arrayOf(
            1,
            14,
            16,
            8,
            10,
            9,
            13,
            24,
            32,
            23,
            33,
            3,
            4,
            6,
            0,
            11,
            18,
            19,
            21,
            7,
            27,
            15,
            31,
            22,
            26,
            17,
            5,
            20,
            30,
            2,
            28,
            29,
            25,
            12
        )
        indices =
            indices.mapIndexed { index, _ -> indices.indexOfFirst { it == index } }.toTypedArray()
        return indices.map { validPackageId[it] }.joinToString("")
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun createKey(context: Context): String {
        return context.packageName.reversed().substring(0, 32)
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun getHackedAppMessage(context: Context): String {
        return SecurityUtils.Aes.Ecb.decrypt(
            "ODGiMOwYJl75nxOQNhEp0GPXAweNIW0023Zj9GbJ89PVQ2PKGGaMunROyu4ZpDtqY8SQVubS3ijA5bDJNhVQN/X03zr3cRR+opgpJIIjmr+ziQJk0mk7ByevUeu7EpagbkP+HiTvkQPXvYnALzCleNb/rGyHSw83eKuvJ2SBEfch4qcpBz7m3Ylsb4w4VxqPS3ba6Toz/S2lB+Vr1GxijWjtWkpKaHp7ldLxovkvLckJAn6dNPQj0f60fEGMsUl2E9dcLzYBYqOYg52Et93FuiMzB/lZFnHxUluoiVMGGadAFsG5aQv4yBuU/3Zmju/GpRcS6ImZckWP8Kt7kwVaCUOS7GzDr+eQs8Uz67ICZqh1rnCDodsGCi723seDISus4tpNyKvfdsGJipqinqZnJB138u0v6FCNBWXdNKOmZTIvP7YcldWdfGhuV2gCDfVPeGkxPJM4wIOZEgYsuh1HHB+K4O9SvBz1hE/NuP9otEV/Bufya1KejOjMP7ZiAVCUsVL59GY6FJyc3HROBZjCtOedIVCboCPmVAxLHZtn5sBetJ72YaEN/t2CoCB6ZkadkV1kDuEjHbsWUOqcGFf6twmwdewHo6hJ2tYNxP8Gcty1jjPY4fu1M3LlAxnwLrtI",
            createKey(context)
        )
    }

    @Suppress("NOTHING_TO_INLINE")
    fun onNoteAutomaticallyChanged() {
        if (!noteSwitchTrigger && toneAnalyzer.nsc >= 90) {
            noteSwitchTrigger = true
            toneAnalyzer.resetNSC()
            appSettings.setNoteSwitchTrigger(true)
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    fun runAntihackingCheck(activity: Activity) {
        if (appProbablyHacked < 0) {
            return
        }

        if (alertDialog != null) {
            return
        }

        if (!noteSwitchTrigger) {
            return
        }

        if (toneAnalyzer.nsc < 12) {
            return
        }

        val measuredInharmonicityCount = toneAnalyzer.inharmonicity.count { it[0] != 0.0 }
        if (measuredInharmonicityCount < 5) {
            return
        }
        val firstNotNull = toneAnalyzer.inharmonicity.indexOfFirst { it[0] != 0.0 }
        val lastNotNull = toneAnalyzer.inharmonicity.indexOfLast { it[0] != 0.0 }
        if (lastNotNull - firstNotNull < 47) {
            return
        }

        showAntiHackingDialog(activity)
    }

    @Suppress("NOTHING_TO_INLINE")
    fun showAntiHackingDialog(activity: Activity) {
        val handler = Handler(Looper.getMainLooper())
        val token = "timerToken"
        var timerSeconds = 30
        val formatTime = { x: Int -> "00:${x.toString().padStart(2, '0')}" }
        val activeColor = 0xffffffff.toInt()
        val disabledColor = 0xffaaaaaa.toInt()
        OrientationUtils.lockOrientation(activity)
        alertDialog = AlertDialog.Builder(activity)
            .setCancelable(false)
            .setTitle(R.string.app_name)
            .setMessage(getHackedAppMessage(activity))
            .setPositiveButton(formatTime(timerSeconds)) { dialog, _ ->
                if (timerSeconds > 0) {
                    return@setPositiveButton
                }
                dialog.dismiss()
                alertDialog = null
                toneAnalyzer.resetNSC()
            }
            .setNeutralButton("erotS/yalP/nepO".replace("/", " ").reversed()) { dialog, _ ->
                val intent = IntentUtils.openPlayStore(activity, getPackageName())
                if (intent.resolveActivity(activity.packageManager) != null) {
                    activity.startActivity(intent)
                }
                dialog.dismiss()
                alertDialog = null
                toneAnalyzer.resetNSC()
            }
            .setOnDismissListener {
                handler.removeCallbacksAndMessages(token)
                OrientationUtils.unlockOrientation(activity)
                alertDialog = null
                toneAnalyzer.resetNSC()
            }
            .create()
        alertDialog?.show()
        alertDialog?.window?.let {
            val params = it.attributes
            params.width = WindowManager.LayoutParams.WRAP_CONTENT
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            params.gravity = Gravity.TOP
            it.attributes = params
        }
        alertDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
            setTextColor(disabledColor)
            isEnabled = false
        }
        alertDialog?.getButton(AlertDialog.BUTTON_NEUTRAL)?.apply {
            setTextColor(activeColor)
        }

        val runnable = object : Runnable {
            override fun run() {
                if (timerSeconds <= 0) {
                    alertDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                        setText(R.string.action_ok)
                        isEnabled = true
                        setTextColor(activeColor)
                    }
                    handler.removeCallbacksAndMessages(token)
                    return
                }
                alertDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                    text = formatTime.invoke(timerSeconds)
                    setTextColor(disabledColor)
                }
                timerSeconds--
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(runnable)
    }
}