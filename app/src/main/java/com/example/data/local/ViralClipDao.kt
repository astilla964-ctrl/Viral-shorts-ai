package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ViralClipDao {
    @Query("SELECT * FROM viral_clips ORDER BY createdAt DESC")
    fun getAllClips(): Flow<List<ViralClipEntity>>

    @Query("SELECT * FROM viral_clips WHERE id = :id")
    suspend fun getClipById(id: Long): ViralClipEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClip(clip: ViralClipEntity): Long

    @Query("DELETE FROM viral_clips WHERE id = :id")
    suspend fun deleteClipById(id: Long)

    @Query("DELETE FROM viral_clips")
    suspend fun clearAll()
}
