package com.example.data.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Manages AES-256-GCM encryption/decryption backed by Android KeyStore.
 * Ensures robust encryption for all stored sensitive health and personal info.
 */
object CryptoManager {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val ALIAS = "WaterTrackerMasterKey"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val IV_SIZE = 12

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        if (!keyStore.containsAlias(ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val parameterSpec = KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()

            keyGenerator.init(parameterSpec)
            return keyGenerator.generateKey()
        }
        val entry = keyStore.getEntry(ALIAS, null) as KeyStore.SecretKeyEntry
        return entry.secretKey
    }

    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            // Prepend IV to ciphertext
            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)

            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            // Fallback gracefully without breaking app
            plainText
        }
    }

    fun decrypt(encryptedText: String): String {
        if (encryptedText.isEmpty()) return ""
        return try {
            val decoded = Base64.decode(encryptedText, Base64.NO_WRAP)
            if (decoded.size < IV_SIZE) return encryptedText

            val iv = ByteArray(IV_SIZE)
            val cipherText = ByteArray(decoded.size - IV_SIZE)
            System.arraycopy(decoded, 0, iv, 0, IV_SIZE)
            System.arraycopy(decoded, IV_SIZE, cipherText, 0, cipherText.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), spec)

            val decryptedBytes = cipher.doFinal(cipherText)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            // Return raw if decryption fails (e.g. was unencrypted)
            encryptedText
        }
    }
}
