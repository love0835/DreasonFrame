package com.dreason.frame.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dreason.frame.core.database.entity.RuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {

    @Query("SELECT * FROM routing_rules ORDER BY priority DESC, createdAt ASC")
    fun getAll(): Flow<List<RuleEntity>>

    @Query("SELECT * FROM routing_rules WHERE id = :id")
    suspend fun getById(id: Long): RuleEntity?

    @Query("SELECT * FROM routing_rules WHERE type = :type AND enabled = 1 ORDER BY priority DESC")
    suspend fun getEnabledByType(type: String): List<RuleEntity>

    @Query("SELECT * FROM routing_rules WHERE enabled = 1 ORDER BY priority DESC")
    suspend fun getAllEnabled(): List<RuleEntity>

    @Query("SELECT * FROM routing_rules WHERE groupTag = :groupTag")
    fun getByGroupTag(groupTag: String): Flow<List<RuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: RuleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<RuleEntity>)

    @Update
    suspend fun update(rule: RuleEntity)

    @Delete
    suspend fun delete(rule: RuleEntity)

    @Query("DELETE FROM routing_rules WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM routing_rules WHERE isBuiltIn = 1")
    suspend fun deleteAllBuiltIn()

    @Query("SELECT COUNT(*) FROM routing_rules")
    suspend fun count(): Int
}
