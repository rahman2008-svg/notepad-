package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

data class ChecklistItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String = "",
    val isChecked: Boolean = false
)

object ChecklistSerializer {
    fun fromJson(json: String): List<ChecklistItem> {
        if (json.trim() == "[]" || json.trim().isEmpty()) return emptyList()
        val items = mutableListOf<ChecklistItem>()
        try {
            // Regex to parse {"id":"...","text":"...","isChecked":true/false}
            val regex = """\{"id":"(.*?)","text":"(.*?)","isChecked":(true|false)\}""".toRegex()
            regex.findAll(json).forEach { matchResult ->
                val id = matchResult.groupValues[1]
                val text = matchResult.groupValues[2].replace("\\\"", "\"").replace("\\\\", "\\")
                val isChecked = matchResult.groupValues[3].toBoolean()
                items.add(ChecklistItem(id, text, isChecked))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return items
    }

    fun toJson(items: List<ChecklistItem>): String {
        return items.joinToString(prefix = "[", postfix = "]") { item ->
            val escapedText = item.text.replace("\\", "\\\\").replace("\"", "\\\"")
            """{"id":"${item.id}","text":"$escapedText","isChecked":${item.isChecked}}"""
        }
    }
}

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val type: String = "text", // "text" or "checklist"
    val checklistItemsJson: String = "[]",
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isTrashed: Boolean = false,
    val colorIndex: Int = 0, // 0 corresponds to default, others to keep pastel colors
    val label: String = "", // Tag label, e.g. "Work", "Personal"
    val createdTime: Long = System.currentTimeMillis(),
    val modifiedTime: Long = System.currentTimeMillis(),
    val reminderTime: Long? = null,
    val isReminderTriggered: Boolean = false
)
