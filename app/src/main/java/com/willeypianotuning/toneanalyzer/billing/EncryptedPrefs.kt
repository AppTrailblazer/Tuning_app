package com.willeypianotuning.toneanalyzer.billing

import android.content.Context
import com.willeypianotuning.toneanalyzer.billing.security.SecurityUtils
import java.util.*

interface EncryptedPrefs {
    fun putString(key: String, value: String)
    fun putBoolean(key: String, value: Boolean)
    fun getString(key: String, defValue: String): String
    fun getBoolean(key: String, defValue: Boolean): Boolean
}

class EcbEncryptedPrefs(name: String, context: Context): EncryptedPrefs {
    private val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)

    private val eKey: String
        get() = manipulateString(5) + reverseString2("xZgo") + "mr45" + manipulateString(
            3
        ) + manipulateString(11) + "CTF" + manipulateString(2) + reverseString("HTF1")

    private fun encrypt(value: String): String {
        return SecurityUtils.Aes.Ecb.encrypt(value, eKey)
    }

    private fun decrypt(value: String): String {
        return SecurityUtils.Aes.Ecb.decrypt(value, eKey)
    }

    override fun putString(key: String, value: String) {
        prefs.edit()
            .putString(encrypt(key), encrypt(value))
            .apply()
    }

    override fun putBoolean(key: String, value: Boolean) {
        prefs.edit()
            .putString(encrypt(key), encrypt(value.toString()))
            .apply()
    }

    override fun getString(key: String, defValue: String): String {
        val encryptedKey = encrypt(key)
        if (!prefs.contains(encryptedKey)) {
            return defValue
        }
        val encryptedValue = prefs.getString(encryptedKey, null) ?: return defValue
        return decrypt(encryptedValue)
    }

    override fun getBoolean(key: String, defValue: Boolean): Boolean {
        val encryptedKey = encrypt(key)
        if (!prefs.contains(encryptedKey)) {
            return defValue
        }
        val encryptedValue = prefs.getString(encryptedKey, null) ?: return defValue
        return decrypt(encryptedValue).toBoolean()
    }

    private fun reverseString(s: String): String {
        return StringBuilder(s).reverse().toString()
    }

    private fun reverseString2(s: String): String {
        return StringBuilder(s).reverse().toString().uppercase(Locale.US)
    }

    private fun manipulateString(i: Int): String {
        return if (i < 9) "" else (i - 2).toString()
    }
}