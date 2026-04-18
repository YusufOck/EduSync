package com.example.edusync.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    // Room has been removed as per Phase 2 migration to Firebase.
    // Database related provisions are now handled by FirebaseModule.
}
