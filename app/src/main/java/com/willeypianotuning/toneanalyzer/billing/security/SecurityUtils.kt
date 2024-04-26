package com.willeypianotuning.toneanalyzer.billing.security

import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object SecurityUtils {
    object Aes {
        private const val ENCRYPTION_ALGORITHM = "AES"

        @Deprecated("Insecure. Not supported on backend due to mcrypt. Left for backward compatibility")
        object Ecb {
            private const val ENCRYPTION_MODE = "ECB"
            private const val ENCRYPTION_PADDING = "PKCS5Padding"

            fun encrypt(input: String, key: String): String {
                val cipher =
                    Cipher.getInstance("$ENCRYPTION_ALGORITHM/$ENCRYPTION_MODE/$ENCRYPTION_PADDING")
                cipher.init(
                    Cipher.ENCRYPT_MODE,
                    SecretKeySpec(key.toByteArray(), ENCRYPTION_ALGORITHM)
                )
                return Base64.encode(cipher.doFinal(input.toByteArray()))
            }

            fun decrypt(input: String, key: String): String {
                val cipher =
                    Cipher.getInstance("$ENCRYPTION_ALGORITHM/$ENCRYPTION_MODE/$ENCRYPTION_PADDING")
                cipher.init(
                    Cipher.DECRYPT_MODE,
                    SecretKeySpec(key.toByteArray(Charsets.US_ASCII), ENCRYPTION_ALGORITHM)
                )
                val output = cipher.doFinal(Base64.decode(input))
                return String(output)
            }
        }

        object Cbc {
            private const val ENCRYPTION_MODE = "CBC"
            private const val ENCRYPTION_PADDING = "PKCS5Padding"

            fun encrypt(input: String, key: String, iv: String): String {
                val cipher =
                    Cipher.getInstance("$ENCRYPTION_ALGORITHM/$ENCRYPTION_MODE/$ENCRYPTION_PADDING")
                cipher.init(
                    Cipher.ENCRYPT_MODE,
                    SecretKeySpec(key.toByteArray(), ENCRYPTION_ALGORITHM),
                    IvParameterSpec(iv.toByteArray(Charsets.UTF_8))
                )
                return Base64.encode(cipher.doFinal(input.toByteArray()))
            }

            fun decrypt(input: String, key: String, iv: String): String {
                val cipher =
                    Cipher.getInstance("$ENCRYPTION_ALGORITHM/$ENCRYPTION_MODE/$ENCRYPTION_PADDING")
                cipher.init(
                    Cipher.DECRYPT_MODE,
                    SecretKeySpec(key.toByteArray(Charsets.UTF_8), ENCRYPTION_ALGORITHM),
                    IvParameterSpec(iv.toByteArray(Charsets.UTF_8))
                )
                val output = cipher.doFinal(Base64.decode(input))
                return String(output)
            }
        }
    }


}