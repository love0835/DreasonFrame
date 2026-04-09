package com.dreason.frame.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dreason.frame.core.database.entity.ServerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerDao {

    @Query("SELECT * FROM proxy_servers ORDER BY createdAt DESC")
    fun getAll(): Flow<List<ServerEntity>>

    @Query("SELECT * FROM proxy_servers WHERE id = :id")
    suspend fun getById(id: Long): ServerEntity?

    @Query("SELECT * FROM proxy_servers WHERE routeGroup = :routeGroup AND enabled = 1 LIMIT 1")
    suspend fun getActiveByRouteGroup(routeGroup: String): ServerEntity?

    @Query("SELECT * FROM proxy_servers WHERE enabled = 1")
    suspend fun getAllEnabled(): List<ServerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(server: ServerEntity): Long

    @Update
    suspend fun update(server: ServerEntity)

    @Delete
    suspend fun delete(server: ServerEntity)

    @Query("DELETE FROM proxy_servers WHERE id = :id")
    suspend fun deleteById(id: Long)
}
