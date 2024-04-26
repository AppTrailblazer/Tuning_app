package com.willeypianotuning.toneanalyzer.audio.source.util

import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy

@RequiresApi(Build.VERSION_CODES.M)
suspend fun AudioManager.inputDevicesFlow(): Flow<Array<out AudioDeviceInfo>> =
    callbackFlow {
        val callback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
                super.onAudioDevicesAdded(addedDevices)
                trySend(getDevices(AudioManager.GET_DEVICES_INPUTS))
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
                super.onAudioDevicesRemoved(removedDevices)
                trySend(getDevices(AudioManager.GET_DEVICES_INPUTS))
            }
        }
        registerAudioDeviceCallback(callback, null)
        awaitClose { unregisterAudioDeviceCallback(callback) }
    }.distinctUntilChangedBy { it -> it.map { it.id }.toSet() }