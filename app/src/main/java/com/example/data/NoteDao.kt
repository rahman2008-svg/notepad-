package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE isTrashed = 0 AND isArchived = 0 ORDER BY isPinned DESC, modifiedTime DESC")
    fun getActiveNotesFlow(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isArchived = 1 AND isTrashed = 0 ORDER BY modifiedTime DESC")
    fun getArchivedNotesFlow(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isTrashed = 1 ORDER BY modifiedTime DESC")
    fun getTrashedNotesFlow(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE reminderTime IS NOT NULL AND isTrashed = 0 AND isReminderTriggered = 0 ORDER BY reminderTime ASC")
    fun getActiveRemindersFlow(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE reminderTime IS NOT NULL AND isTrashed = 0")
    suspend fun getAllReminders(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE id = :id")
    fun getNoteByIdFlow(id: Long): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE isTrashed = 1")
    suspend fun clearTrash()
}
