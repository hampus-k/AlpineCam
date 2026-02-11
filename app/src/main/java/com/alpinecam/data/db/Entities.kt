package com.alpinecam.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class Discipline {
    SL,  // Slalom
    GS,  // Giant Slalom
    SG,  // Super-G
    DH,  // Downhill
    AC,  // Alpine Combined
}

@Entity(tableName = "groups")
data class SkierGroup(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sortOrder: Int = 0,
)

@Entity(
    tableName = "skiers",
    foreignKeys = [
        ForeignKey(
            entity = SkierGroup::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("groupId")],
)
data class Skier(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val groupId: Long? = null,
)

@Entity(tableName = "training_sessions")
data class TrainingSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,  // epoch millis
    val discipline: Discipline,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "session_skiers",
    primaryKeys = ["sessionId", "skierId"],
    foreignKeys = [
        ForeignKey(
            entity = TrainingSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Skier::class,
            parentColumns = ["id"],
            childColumns = ["skierId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("skierId")],
)
data class SessionSkier(
    val sessionId: Long,
    val skierId: Long,
)

@Entity(
    tableName = "video_recordings",
    foreignKeys = [
        ForeignKey(
            entity = TrainingSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Skier::class,
            parentColumns = ["id"],
            childColumns = ["skierId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId"), Index("skierId")],
)
data class VideoRecording(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val skierId: Long,
    val videoUri: String,
    val durationSeconds: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = "",
)
