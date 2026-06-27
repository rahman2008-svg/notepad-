package com.example.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.data.AppDatabase
import com.example.data.NoteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        if (action == Intent.ACTION_BOOT_COMPLETED) {
            // Smart feature: Reschedule all active reminders on reboot!
            val db = AppDatabase.getDatabase(context)
            val repository = NoteRepository(db.noteDao())
            CoroutineScope(Dispatchers.IO).launch {
                val reminders = repository.getAllReminders()
                val currentTime = System.currentTimeMillis()
                for (note in reminders) {
                    val reminderTime = note.reminderTime
                    if (reminderTime != null && !note.isReminderTriggered && !note.isTrashed) {
                        if (reminderTime > currentTime) {
                            ReminderManager.scheduleReminder(context, note.id, reminderTime, note.title, note.content)
                        } else {
                            // If reminder time has passed while phone was off, trigger it immediately or mark it
                            ReminderManager.scheduleReminder(context, note.id, currentTime + 1000, note.title, note.content)
                        }
                    }
                }
            }
            return
        }

        // Standard alarm trigger
        val noteId = intent.getLongExtra("NOTE_ID", -1L)
        val noteTitle = intent.getStringExtra("NOTE_TITLE") ?: "NotePad+ Reminder"
        val noteContent = intent.getStringExtra("NOTE_CONTENT") ?: "Active notepad reminder"

        if (noteId != -1L) {
            val serviceIntent = Intent(context, RingtoneService::class.java).apply {
                putExtra("NOTE_ID", noteId)
                putExtra("NOTE_TITLE", noteTitle)
                putExtra("NOTE_CONTENT", noteContent)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}
