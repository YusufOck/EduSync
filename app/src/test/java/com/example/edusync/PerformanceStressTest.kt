package com.example.edusync

import com.example.edusync.data.Teacher
import com.example.edusync.data.TeacherAvailability
import com.example.edusync.util.SecurityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureTimeMillis

class PerformanceStressTest {

    @Test
    fun `simulate 10 concurrent clients processing heavy decrypt operations`() = runBlocking {
        println("Starting Stress Test for 10 Concurrent Devices Simulation...")

        // Her biri 100 öğretmen verisine ve 500 mesaja sahip 10 cihaz eşzamanlı çalışıyor.
        val clientsCount = 10
        val dataPerClient = 1000

        // Mock encrypted payloads
        val sampleTeacher = Teacher(id = 1, name = "John", surname = "Doe", title = "Prof")
        val plaintext = "Test Data Payload #123456789"
        val encryptedText = SecurityUtils.encrypt(plaintext)
        
        println("Generated Encrypted Payload: $encryptedText")
        
        val timeTaken = measureTimeMillis {
            coroutineScope {
                val jobs = (1..clientsCount).map { clientId ->
                    async(Dispatchers.Default) {
                        for (i in 1..dataPerClient) {
                            // Cihazlar arka planda yüzlerce datayı şifreliyor ve çözüyor (Firebase simülasyonu)
                            val decoded = SecurityUtils.decrypt(encryptedText)
                            val newEncrypted = SecurityUtils.encrypt(decoded + clientId)
                            SecurityUtils.decrypt(newEncrypted)
                        }
                    }
                }
                jobs.awaitAll()
            }
        }

        println("Simulation completed in $timeTaken ms")
        // Eski kod bu yükte kilitlenir veya dakikalar sürerdi
        assertTrue("Test took too long! Optimization failed.", timeTaken < 3000)
    }
}
