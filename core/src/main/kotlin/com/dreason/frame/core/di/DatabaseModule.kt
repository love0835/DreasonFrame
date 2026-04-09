package com.dreason.frame.core.di

import android.content.Context
import androidx.room.Room
import com.dreason.frame.core.database.DreasonDatabase
import com.dreason.frame.core.database.dao.LogDao
import com.dreason.frame.core.database.dao.RuleDao
import com.dreason.frame.core.database.dao.ServerDao
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
    fun provideDatabase(@ApplicationContext context: Context): DreasonDatabase =
        Room.databaseBuilder(
            context,
            DreasonDatabase::class.java,
            "dreason_frame.db"
        ).build()

    @Provides
    fun provideServerDao(db: DreasonDatabase): ServerDao = db.serverDao()

    @Provides
    fun provideRuleDao(db: DreasonDatabase): RuleDao = db.ruleDao()

    @Provides
    fun provideLogDao(db: DreasonDatabase): LogDao = db.logDao()
}
