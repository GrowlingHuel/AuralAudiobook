package com.vibe.acousticalchemy

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

import androidx.core.app.ServiceCompat
import android.media.AudioManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest

import android.support.v4.media.session.MediaSessionCompat
import androidx.media.app.NotificationCompat.MediaStyle

class NarrationService : Service() {

    private lateinit var mediaSession: MediaSessionCompat

    companion object {
        @Volatile
        var isReady = false
        var instance: NarrationService? = null
    }

    inner class LocalBinder : android.os.Binder() {
        fun getService(): NarrationService = this@NarrationService
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize MediaSession (Critical for Android 16 Attribution)
        mediaSession = MediaSessionCompat(this, "NarrationSession")
        mediaSession.isActive = true
        
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Immediate Foreground Promotion (Critical for CPU Priority)
        val notification = createNotification()
        ServiceCompat.startForeground(this, 1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        
        // Set Thread Priority to AUDIO (as requested for Service, though it affects main)
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO)
        
        // Request Audio Focus
        if (requestAudioFocus()) {
             isReady = true
        } else {
             stopSelf()
        }
        
        return START_STICKY
    }
    
    private fun requestAudioFocus(): Boolean {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener { /* Handle focus change if needed */ }
                .build()
            audioManager.requestAudioFocus(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                { },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }
    
    override fun onDestroy() {
        super.onDestroy()
        isReady = false
        instance = null
        mediaSession.release()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "narration_channel",
                "Audiobook Playback",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "narration_channel")
            .setContentTitle("Aural Adventures")
            .setContentText("Speaking...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setStyle(MediaStyle().setMediaSession(mediaSession.sessionToken))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)
            .build()
    }
}
