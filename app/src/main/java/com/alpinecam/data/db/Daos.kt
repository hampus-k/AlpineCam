package com.alpinecam.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// --- Group DAO ---

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups ORDER BY sortOrder, name")
    fun getAll(): Flow<List<SkierGroup>>

    @Query("SELECT * FROM groups WHERE id = :id")
    suspend fun getById(id: Long): SkierGroup?

    @Insert
    suspend fun insert(group: SkierGroup): Long

    @Update
    suspend fun update(group: SkierGroup)

    @Delete
    suspend fun delete(group: SkierGroup)
}

// --- Skier DAO ---

data class SkierWithGroup(
    val id: Long,
    val name: String,
    val groupId: Long?,
    val groupName: String?,
)

@Dao
interface SkierDao {
    @Query("""
        SELECT s.id, s.name, s.groupId, g.name AS groupName
        FROM skiers s LEFT JOIN groups g ON s.groupId = g.id
        ORDER BY s.name
    """)
    fun getAllWithGroup(): Flow<List<SkierWithGroup>>

    @Query("""
        SELECT s.id, s.name, s.groupId, g.name AS groupName
        FROM skiers s LEFT JOIN groups g ON s.groupId = g.id
        WHERE s.groupId = :groupId
        ORDER BY s.name
    """)
    fun getByGroup(groupId: Long): Flow<List<SkierWithGroup>>

    @Query("SELECT * FROM skiers WHERE id = :id")
    suspend fun getById(id: Long): Skier?

    @Insert
    suspend fun insert(skier: Skier): Long

    @Update
    suspend fun update(skier: Skier)

    @Delete
    suspend fun delete(skier: Skier)
}

// --- Training Session DAO ---

data class SessionWithDetails(
    val id: Long,
    val date: Long,
    val discipline: Discipline,
    val notes: String,
    val createdAt: Long,
    val skierCount: Int,
    val videoCount: Int,
)

@Dao
interface TrainingSessionDao {
    @Query("""
        SELECT ts.*,
            (SELECT COUNT(*) FROM session_skiers ss WHERE ss.sessionId = ts.id) AS skierCount,
            (SELECT COUNT(*) FROM video_recordings vr WHERE vr.sessionId = ts.id) AS videoCount
        FROM training_sessions ts
        ORDER BY ts.date DESC
    """)
    fun getAllWithDetails(): Flow<List<SessionWithDetails>>

    @Query("SELECT * FROM training_sessions WHERE id = :id")
    suspend fun getById(id: Long): TrainingSession?

    @Insert
    suspend fun insert(session: TrainingSession): Long

    @Update
    suspend fun update(session: TrainingSession)

    @Delete
    suspend fun delete(session: TrainingSession)

    // Session-Skier linking
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSkierToSession(link: SessionSkier)

    @Delete
    suspend fun removeSkierFromSession(link: SessionSkier)

    @Query("""
        SELECT s.id, s.name, s.groupId, g.name AS groupName
        FROM skiers s
        LEFT JOIN groups g ON s.groupId = g.id
        INNER JOIN session_skiers ss ON ss.skierId = s.id
        WHERE ss.sessionId = :sessionId
        ORDER BY s.name
    """)
    fun getSkiersForSession(sessionId: Long): Flow<List<SkierWithGroup>>
}

// --- Video Recording DAO ---

data class VideoWithDetails(
    val id: Long,
    val sessionId: Long,
    val skierId: Long,
    val skierName: String,
    val videoUri: String,
    val durationSeconds: Int,
    val timestamp: Long,
    val notes: String,
    val discipline: Discipline,
    val sessionDate: Long,
)

@Dao
interface VideoRecordingDao {
    @Query("""
        SELECT vr.*, s.name AS skierName, ts.discipline, ts.date AS sessionDate
        FROM video_recordings vr
        INNER JOIN skiers s ON vr.skierId = s.id
        INNER JOIN training_sessions ts ON vr.sessionId = ts.id
        WHERE vr.sessionId = :sessionId
        ORDER BY vr.timestamp DESC
    """)
    fun getBySession(sessionId: Long): Flow<List<VideoWithDetails>>

    @Query("""
        SELECT vr.*, s.name AS skierName, ts.discipline, ts.date AS sessionDate
        FROM video_recordings vr
        INNER JOIN skiers s ON vr.skierId = s.id
        INNER JOIN training_sessions ts ON vr.sessionId = ts.id
        WHERE vr.skierId = :skierId
        ORDER BY vr.timestamp DESC
    """)
    fun getBySkier(skierId: Long): Flow<List<VideoWithDetails>>

    @Insert
    suspend fun insert(recording: VideoRecording): Long

    @Delete
    suspend fun delete(recording: VideoRecording)
}
