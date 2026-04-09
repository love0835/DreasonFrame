package com.dreason.frame.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "routing_rules",
    indices = [Index("type"), Index("priority")]
)
data class RuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,
    val pattern: String,
    val route: String,
    val priority: Int = 0,
    val enabled: Boolean = true,
    val isBuiltIn: Boolean = false,
    val groupTag: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
