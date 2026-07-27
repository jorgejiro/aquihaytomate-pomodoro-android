package com.jjrapps.aquihaytomate.di

import android.content.Context
import androidx.room.Room
import com.jjrapps.aquihaytomate.data.local.db.AppDatabase
import com.jjrapps.aquihaytomate.data.local.db.FocusSessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * No `fallbackToDestructiveMigration`: losing a user's focus history to a schema bump is not an
     * acceptable outcome. Every version increment carries a real migration and its test.
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME).build()

    @Provides
    fun provideFocusSessionDao(database: AppDatabase): FocusSessionDao = database.focusSessionDao()
}
