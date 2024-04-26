package com.willeypianotuning.toneanalyzer.audio.source.util

import android.media.AudioDeviceInfo
import android.os.Build
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.M)
fun AudioDeviceInfo.isBuiltInMic(): Boolean {
    return isSource && type == AudioDeviceInfo.TYPE_BUILTIN_MIC
}

@RequiresApi(Build.VERSION_CODES.M)
fun AudioDeviceInfo.isExternalMic(): Boolean {
    return isSource && (type == AudioDeviceInfo.TYPE_WIRED_HEADSET || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && type == AudioDeviceInfo.TYPE_USB_HEADSET))
}

@RequiresApi(Build.VERSION_CODES.M)
fun AudioDeviceInfo.isBluetoothMic(): Boolean {
    return isSource && (type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && type == AudioDeviceInfo.TYPE_BLE_HEADSET))
}

@RequiresApi(Build.VERSION_CODES.M)
fun AudioDeviceInfo.toLogString(): String {
    val props = mutableMapOf<String, String>()
    props["id"] = id.toString()
    props["name"] = productName.toString()
    props["type"] = type.toString()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        props["address"] = this.address
    }
    return "{" + props.map { "${it.key}=${it.value}" }.joinToString(", ") + "}"
}