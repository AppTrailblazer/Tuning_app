package com.willeypianotuning.toneanalyzer.billing

import com.willeypianotuning.toneanalyzer.billing.security.Base64
import java.security.MessageDigest

fun String.sha512() = hashString("SHA-512", this)

fun String.sha256() = hashString("SHA-256", this)

fun String.sha1() = hashString("SHA-1", this)

fun String.encodeBase64(): String = Base64.encode(this.toByteArray())

fun String.decodeBase64(): String = String(Base64.decode(this.toByteArray()))

/**
 * Supported algorithms on Android:
 *
 * Algorithm	Supported API Levels
 * MD5          1+
 * SHA-1	    1+
 * SHA-224	    1-8,22+
 * SHA-256	    1+
 * SHA-384	    1+
 * SHA-512	    1+
 */
private fun hashString(type: String, input: String): String {
    val HEX_CHARS = "0123456789ABCDEF"
    val bytes = MessageDigest
        .getInstance(type)
        .digest(input.toByteArray())
    val result = StringBuilder(bytes.size * 2)

    bytes.forEach {
        val i = it.toInt()
        result.append(HEX_CHARS[i shr 4 and 0x0f])
        result.append(HEX_CHARS[i and 0x0f])
    }

    return result.toString()
}