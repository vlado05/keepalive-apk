package com.test.keepalive

import android.app.*
import android.content.Intent
import android.media.*
import android.os.*
import android.widget.*
import kotlin.concurrent.thread

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val btn = Button(this)
        btn.text = "START KEEPALIVE"

        btn.setOnClickListener {
            startService(Intent(this, KeepAliveService::class.java))
        }

        setContentView(btn)
    }
}

class KeepAliveService : Service() {

    private var running = false
    private var track: AudioTrack? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        startForeground(1, createNotification())
        startAudio()

        return START_STICKY
    }

    private fun startAudio() {

        if (running) return
        running = true

        val sampleRate = 48000

        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        track = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize,
            AudioTrack.MODE_STREAM
        )

        track?.play()

        val buffer = ShortArray(bufferSize)

        thread {

            while (running) {

                for (i in buffer.indices) {
                    val noise = (Math.random() * 2 - 1)
                    val sample = (noise * 0.00015 * Short.MAX_VALUE).toInt()
                    buffer[i] = sample.toShort()
                }

                track?.write(buffer, 0, buffer.size)
            }
        }
    }

    private fun createNotification(): Notification {

        val channelId = "keepalive"

        val channel = NotificationChannel(
            channelId,
            "KeepAlive",
            NotificationManager.IMPORTANCE_LOW
        )

        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)

        return Notification.Builder(this, channelId)
            .setContentTitle("KeepAlive running")
            .setContentText("Audio active")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()
    }

    override fun onDestroy() {
        running = false
        track?.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
