package com.example.data.repository

import com.example.data.local.ViralClipDao
import com.example.data.local.ViralClipEntity
import kotlinx.coroutines.flow.Flow

class ViralShortsRepository(private val dao: ViralClipDao) {
    val allClips: Flow<List<ViralClipEntity>> = dao.getAllClips()

    suspend fun getClipById(id: Long): ViralClipEntity? = dao.getClipById(id)

    suspend fun saveClip(clip: ViralClipEntity): Long = dao.insertClip(clip)

    suspend fun deleteClip(id: Long) = dao.deleteClipById(id)

    suspend fun clearAll() = dao.clearAll()
}
