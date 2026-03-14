package com.example.edusync.di

import android.content.Context
import androidx.room.Room
import com.example.edusync.data.EduSyncDatabase
import com.example.edusync.data.TeacherDao
import com.example.edusync.data.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): EduSyncDatabase {
        return Room.databaseBuilder(
            context,
            EduSyncDatabase::class.java,
            "edusync_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideTeacherDao(database: EduSyncDatabase): TeacherDao {
        return database.teacherDao()
    }

    @Provides
    fun provideUserDao(database: EduSyncDatabase): UserDao {
        return database.userDao()
    }
}
