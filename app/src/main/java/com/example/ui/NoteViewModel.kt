package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ChecklistItem
import com.example.data.ChecklistSerializer
import com.example.data.NoteEntity
import com.example.data.NoteRepository
import com.example.reminder.ReminderManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: NoteRepository

    // Search and filtering criteria
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedFilterColor = MutableStateFlow<Int?>(null)
    val selectedFilterColor = _selectedFilterColor.asStateFlow()

    private val _selectedFilterLabel = MutableStateFlow<String?>(null)
    val selectedFilterLabel = _selectedFilterLabel.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = NoteRepository(database.noteDao())
    }

    // Active, archived, and trashed notes filtered dynamically by query criteria
    val activeNotes: StateFlow<List<NoteEntity>> = combine(
        repository.activeNotesFlow,
        _searchQuery,
        _selectedFilterColor,
        _selectedFilterLabel
    ) { notes, query, colorIndex, label ->
        notes.filter { note ->
            val matchesQuery = query.isEmpty() ||
                    note.title.contains(query, ignoreCase = true) ||
                    note.content.contains(query, ignoreCase = true)
            val matchesColor = colorIndex == null || note.colorIndex == colorIndex
            val matchesLabel = label == null || note.label == label
            matchesQuery && matchesColor && matchesLabel
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedNotes: StateFlow<List<NoteEntity>> = combine(
        repository.archivedNotesFlow,
        _searchQuery
    ) { notes, query ->
        notes.filter { note ->
            query.isEmpty() ||
                    note.title.contains(query, ignoreCase = true) ||
                    note.content.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashedNotes: StateFlow<List<NoteEntity>> = repository.trashedNotesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Extract all unique labels from active and archived notes
    val allLabels: StateFlow<List<String>> = combine(
        repository.activeNotesFlow,
        repository.archivedNotesFlow
    ) { active, archived ->
        (active + archived)
            .map { it.label }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterColor(colorIndex: Int?) {
        _selectedFilterColor.value = colorIndex
    }

    fun setFilterLabel(label: String?) {
        _selectedFilterLabel.value = label
    }

    fun getNoteById(id: Long): Flow<NoteEntity?> {
        return repository.getNoteByIdFlow(id)
    }

    fun saveNote(note: NoteEntity, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val updatedNote = note.copy(modifiedTime = System.currentTimeMillis())
            val id = repository.insertNote(updatedNote)
            onSaved(id)
        }
    }

    fun togglePin(note: NoteEntity) {
        viewModelScope.launch {
            repository.insertNote(note.copy(isPinned = !note.isPinned))
        }
    }

    fun toggleArchive(note: NoteEntity) {
        viewModelScope.launch {
            // If archiving, unpin
            val isArchived = !note.isArchived
            val isPinned = if (isArchived) false else note.isPinned
            repository.insertNote(note.copy(isArchived = isArchived, isPinned = isPinned))
        }
    }

    fun moveToTrash(note: NoteEntity) {
        viewModelScope.launch {
            // Cancel active reminder if trashed
            if (note.reminderTime != null) {
                ReminderManager.cancelReminder(getApplication(), note.id)
            }
            repository.insertNote(note.copy(isTrashed = true, isPinned = false, reminderTime = null))
        }
    }

    fun restoreFromTrash(note: NoteEntity) {
        viewModelScope.launch {
            repository.insertNote(note.copy(isTrashed = false))
        }
    }

    fun deleteNotePermanently(note: NoteEntity) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.clearTrash()
        }
    }

    fun updateNoteColor(note: NoteEntity, colorIndex: Int) {
        viewModelScope.launch {
            repository.insertNote(note.copy(colorIndex = colorIndex))
        }
    }

    fun setNoteReminder(note: NoteEntity, timeMs: Long) {
        viewModelScope.launch {
            // We first save to make sure it's in DB
            val noteToSave = note.copy(reminderTime = timeMs, isReminderTriggered = false)
            val noteId = repository.insertNote(noteToSave)
            
            // Schedule the alarm using ReminderManager
            val noteTitle = if (noteToSave.title.isEmpty()) "NotePad+ Reminder" else noteToSave.title
            val noteContent = if (noteToSave.type == "checklist") {
                val items = ChecklistSerializer.fromJson(noteToSave.checklistItemsJson)
                val unchecked = items.filter { !it.isChecked }.map { it.text }
                if (unchecked.isNotEmpty()) unchecked.first() else "No unchecked items"
            } else {
                if (noteToSave.content.isEmpty()) "You scheduled a reminder" else noteToSave.content
            }

            ReminderManager.scheduleReminder(
                getApplication(),
                noteId,
                timeMs,
                noteTitle,
                noteContent
            )
        }
    }

    fun cancelNoteReminder(note: NoteEntity) {
        viewModelScope.launch {
            ReminderManager.cancelReminder(getApplication(), note.id)
            repository.insertNote(note.copy(reminderTime = null, isReminderTriggered = false))
        }
    }

    fun updateChecklistItemChecked(note: NoteEntity, itemId: String, isChecked: Boolean) {
        viewModelScope.launch {
            val items = ChecklistSerializer.fromJson(note.checklistItemsJson).map {
                if (it.id == itemId) it.copy(isChecked = isChecked) else it
            }
            val serialized = ChecklistSerializer.toJson(items)
            repository.insertNote(note.copy(checklistItemsJson = serialized))
        }
    }
}
