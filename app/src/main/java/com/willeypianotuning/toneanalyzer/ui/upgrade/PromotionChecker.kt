package com.willeypianotuning.toneanalyzer.ui.upgrade

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class PromotionChecker {

    suspend fun checkForPromotion(): String = suspendCoroutine { continuation ->
        try {
            val remoteConfig = FirebaseRemoteConfig.getInstance()
            remoteConfig.fetchAndActivate().addOnCompleteListener {
                continuation.resume(remoteConfig.getString("promo_text"))
            }
        } catch (e: Throwable) {
            continuation.resumeWithException(e)
        }
    }

}