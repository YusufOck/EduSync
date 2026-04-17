package com.example.edusync.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase {
        // En güncel URL'yi buraya ekledik
        val databaseUrl = "https://edusync-e905f-default-rtdb.europe-west1.firebasedatabase.app/" 
        return FirebaseDatabase.getInstance(databaseUrl).apply {
            // Week 10 - Asenkron veri tutarlılığı ve offline destek
            setPersistenceEnabled(true)
        }
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }
}
