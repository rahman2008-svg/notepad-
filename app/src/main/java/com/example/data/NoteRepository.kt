package com.example.data

import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {
    val activeNotesFlow: Flow<List<NoteEntity>> = noteDao.getActiveNotesFlow()
    val archivedNotesFlow: Flow<List<NoteEntity>> = noteDao.getArchivedNotesFlow()
    val trashedNotesFlow: Flow<List<NoteEntity>> = noteDao.getTrashedNotesFlow()
    val activeRemindersFlow: Flow<List<NoteEntity>> = noteDao.getActiveRemindersFlow()

    fun getNoteByIdFlow(id: Long): Flow<NoteEntity?> = noteDao.getNoteByIdFlow(id)

    suspend fun getNoteById(id: Long): NoteEntity? = noteDao.getNoteById(id)

    suspend fun insertNote(note: NoteEntity): Long = noteDao.insertNote(note)

    suspend fun deleteNote(note: NoteEntity) = noteDao.deleteNote(note)

    suspend fun clearTrash() = noteDao.clearTrash()

    suspend fun getAllReminders(): List<NoteEntity> = noteDao.getAllReminders()
}
