package com.archiaipro.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DesignDao {
    @Query("SELECT * FROM designs WHERE projectId = :projectId AND kind = :kind LIMIT 1")
    fun observe(projectId: Long, kind: String): Flow<DesignEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(design: DesignEntity)

    @Query("DELETE FROM designs WHERE projectId = :projectId AND kind = :kind")
    suspend fun delete(projectId: Long, kind: String)
}
