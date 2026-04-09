package com.dreason.frame.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.dreason.frame.core.database.entity.LogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {

    @Query("SELECT * FROM connection_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int = 200): Flow<List<LogEntity>>

    @Query("SELECT * FROM connection_logs WHERE sourceApp = :packageName ORDER BY timestamp DESC LIMIT :limit")
    fun getByApp(packageName: String, limit: Int = 100): Flow<List<LogEntity>>

    @Query("SELECT * FROM connection_logs WHERE route = :route ORDER BY timestamp DESC LIMIT :limit")
    fun getByRoute(route: String, limit: Int = 100): Flow<List<LogEntity>>

    @Insert
    suspend fun insert(log: LogEntity): Long

    @Insert
    suspend fun insertAll(logs: List<LogEntity>)

    @Query("DELETE FROM connection_logs WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("DELETE FROM connection_logs")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM connection_logs")
    suspend fun count(): Int

    @Query("""
        SELECT SUM(bytesSent + bytesReceived)
        FROM connection_logs
        WHERE route = :route AND timestamp > :since
    """)
    suspend fun getTotalBytesByRoute(route: String, since: Long): Long?
}
