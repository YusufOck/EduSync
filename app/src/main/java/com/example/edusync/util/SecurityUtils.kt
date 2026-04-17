package com.example.edusync.util

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object SecurityUtils {
    // Phase 2 - Week 10: Data Security
    private const val AES_KEY = "EduSyncSecretKey1234567890123456" 
    private const val IV = "1234567890123456" // Gerçek uygulamada rastgele üretilmelidir

    /**
     * Şifreler için Tek Yönlı Hash (SHA-256)
     */
    fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    /**
     * Hassas veriler için İki Yönlü Şifreleme (AES/CBC/PKCS5Padding)
     */
    fun encrypt(text: String?): String {
        if (text.isNullOrEmpty()) return ""
        return try {
            val keySpec = SecretKeySpec(AES_KEY.toByteArray(), "AES")
            val ivSpec = IvParameterSpec(IV.toByteArray())
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
            val encryptedBytes = cipher.doFinal(text.toByteArray())
            Base64.encodeToString(encryptedBytes, Base64.DEFAULT).trim()
        } catch (e: Exception) {
            text
        }
    }

    /**
     * Şifreli veriyi çözme (AES Decrypt)
     */
    fun decrypt(encryptedText: String?): String {
        if (encryptedText.isNullOrEmpty()) return ""
        return try {
            val keySpec = SecretKeySpec(AES_KEY.toByteArray(), "AES")
            val ivSpec = IvParameterSpec(IV.toByteArray())
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
            val decodedBytes = Base64.decode(encryptedText, Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            String(decryptedBytes)
        } catch (e: Exception) {
            // Eğer veri şifreli değilse (eski veriler), olduğu gibi döndür
            encryptedText
        }
    }
}
