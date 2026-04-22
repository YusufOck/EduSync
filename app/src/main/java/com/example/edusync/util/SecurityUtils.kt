package com.example.edusync.util

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * PDF Optimization: Thread-safe, non-blocking Cipher pool.
 * Coroutines share Dispatcher threads. Synchronizing a single Cipher halts the entire thread pool.
 * Creating new Ciphers every time is CPU intensive.
 * We use ConcurrentLinkedQueue to maintain a pool of reusable Cipher instances.
 */
object SecurityUtils {
    private const val AES_KEY = "EduSyncSecretKey1234567890123456" 
    private const val IV = "1234567890123456"

    private val keySpec = SecretKeySpec(AES_KEY.toByteArray(Charsets.UTF_8), "AES")
    private val ivSpec = IvParameterSpec(IV.toByteArray(Charsets.UTF_8))

    private val encryptCipherPool = ConcurrentLinkedQueue<Cipher>()
    private val decryptCipherPool = ConcurrentLinkedQueue<Cipher>()

    private fun getEncryptCipher(): Cipher {
        return encryptCipherPool.poll() ?: Cipher.getInstance("AES/CBC/PKCS5Padding").apply {
            init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
        }
    }

    private fun getDecryptCipher(): Cipher {
        return decryptCipherPool.poll() ?: Cipher.getInstance("AES/CBC/PKCS5Padding").apply {
            init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
        }
    }

    private fun releaseEncryptCipher(cipher: Cipher) {
        encryptCipherPool.offer(cipher)
    }

    private fun releaseDecryptCipher(cipher: Cipher) {
        decryptCipherPool.offer(cipher)
    }

    fun hashPassword(password: String): String {
        return try {
            val bytes = password.toByteArray(Charsets.UTF_8)
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            digest.fold("") { str, it -> str + "%02x".format(it) }
        } catch (e: Exception) {
            password
        }
    }

    fun encrypt(text: String?): String {
        if (text.isNullOrEmpty()) return ""
        var cipher: Cipher? = null
        return try {
            cipher = getEncryptCipher()
            val encryptedBytes = cipher.doFinal(text.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            text ?: ""
        } finally {
            if (cipher != null) {
                releaseEncryptCipher(cipher)
            }
        }
    }

    fun decrypt(encryptedText: String?): String {
        if (encryptedText.isNullOrEmpty()) return ""
        var cipher: Cipher? = null
        return try {
            cipher = getDecryptCipher()
            val decodedBytes = Base64.decode(encryptedText, Base64.NO_WRAP)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            encryptedText ?: ""
        } finally {
            if (cipher != null) {
                releaseDecryptCipher(cipher)
            }
        }
    }
}
