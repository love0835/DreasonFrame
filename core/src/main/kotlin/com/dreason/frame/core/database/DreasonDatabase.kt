package com.dreason.frame.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dreason.frame.core.database.converter.Converters
import com.dreason.frame.core.database.dao.LogDao
import com.dreason.frame.core.database.dao.RuleDao
import com.dreason.frame.core.database.dao.ServerDao
import com.dreason.frame.core.database.entity.LogEntity
import com.dreason.frame.core.database.entity.RuleEntity
import com.dreason.frame.core.database.entity.ServerEntity

@Database(
    entities = [
        ServerEntity::class,
        RuleEntity::class,
        LogEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class DreasonDatabase : RoomDatabase() {
    abstract fun serverDao(): ServerDao
    abstract fun ruleDao(): RuleDao
    abstract fun logDao(): LogDao
}
