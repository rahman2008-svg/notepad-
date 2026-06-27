package com.example.reminder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.NoteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RingtoneService : Service() {
    private var ringtone: Ringtone? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val noteId = intent?.getLongExtra("NOTE_ID", -1L) ?: -1L
        val noteTitle = intent?.getStringExtra("NOTE_TITLE") ?: "NotePad+ Reminder"
        val noteContent = intent?.getStringExtra("NOTE_CONTENT") ?: "Active notepad reminder"
        val action = intent?.action

        if (action == "STOP_RINGING") {
            stopRingingAndService(noteId)
            return START_NOT_STICKY
        }

        // Start playing the alarm ringtone
        startRinging()

        // Show foreground notification
        showForegroundNotification(noteId, noteTitle, noteContent)

        return START_STICKY
    }

    private fun startRinging() {
        if (ringtone == null) {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ringtone = RingtoneManager.getRingtone(applicationContext, alarmUri)?.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                }
                play()
            }
        } else if (ringtone?.isPlaying == false) {
            ringtone?.play()
        }
    }

    private fun showForegroundNotification(noteId: Long, title: String, content: String) {
        val channelId = "notepad_reminder_channel_id"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "NotePad+ Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "NotePad+ Reminders & Alarms"
                setBypassDnd(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Action Intent to open the note editor
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("OPEN_NOTE_ID", noteId)
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            noteId.toInt() + 1000,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action Intent to dismiss the ringing sound
        val dismissIntent = Intent(this, RingtoneService::class.java).apply {
            action = "STOP_RINGING"
            putExtra("NOTE_ID", noteId)
        }
        val dismissPendingIntent = PendingIntent.getService(
            this,
            noteId.toInt() + 2000,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(openPendingIntent, true)
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(9999, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(9999, notification)
        }
    }

    private fun stopRingingAndService(noteId: Long) {
        ringtone?.apply {
            if (isPlaying) stop()
        }
        ringtone = null

        if (noteId != -1L) {
            val db = AppDatabase.getDatabase(applicationContext)
            val repository = NoteRepository(db.noteDao())
            CoroutineScope(Dispatchers.IO).launch {
                repository.getNoteById(noteId)?.let { note ->
                    repository.insertNote(note.copy(isReminderTriggered = true))
                }
            }
        }

        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        ringtone?.apply {
            if (isPlaying) stop()
        }
        ringtone = null
        super.onDestroy()
    }
}
