package com.willeypianotuning.toneanalyzer.billing.security

import android.app.Application
import android.util.Log
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class SecurityUtilsTest {
    @Test
    fun testEncryptionAndDecryption() {
        val plainText = "Hello World"
        val key = "MIGeMA0GCSqGSIb3Adc9Fb5D"
        val iv = "MA0GC8SqGSIb3Adc"
        val cipherText = SecurityUtils.Aes.Cbc.encrypt(plainText, key, iv)
        assertEquals(plainText, SecurityUtils.Aes.Cbc.decrypt(cipherText, key, iv))
    }

    @Test
    fun testDecryption() {
        val cipherText = "cNhrPouNz/7nuAbkT1l7jaP0HTJ1WTagJP7L593+c/JQ0W37ZQhsLPF3+CDyUwH+7LZfnBIdmVgGVriKu6bWdA=="
        val key = "OGZXmr459CTF1FTH"
        val iv = "boKRNTYYNp7AiOvY"
        assertEquals("{\"code\":400,\"message\":\"Please pass required parameters\"}", SecurityUtils.Aes.Cbc.decrypt(cipherText, key, iv))
    }

    @Test
    fun encryptSignatures() {
        val secret = "com.willeypianotuning.toneanalyzer".reversed().substring(0, 32)
        Log.v("AppSecurity", SecurityUtils.Aes.Ecb.encrypt("63:D2:AD:CF:B9:44:27:02:8F:75:3D:44:97:10:00:74:6F:AA:F4:6D", secret))
        Log.v("AppSecurity", SecurityUtils.Aes.Ecb.encrypt("63:D7:C0:C9:71:CF:15:C0:9B:05:C7:DB:62:14:B9:97:DE:5D:2E:89", secret))

        val appHackedMessage = "It looks like you might be using an unauthorized version of PianoMeter. If this app has helped you to tune a piano, please consider downloading and unlocking a licensed version of PianoMeter directly from the Google Play Store. Doing so will ensure that you always stay updated with the newest releases, and you can be assured that the app will not have any 3rd party malware. Your support will also help make the continuing development and maintenance of PianoMeter sustainable"
        Log.v("AppSecurity", SecurityUtils.Aes.Ecb.encrypt(appHackedMessage, secret))
    }

}