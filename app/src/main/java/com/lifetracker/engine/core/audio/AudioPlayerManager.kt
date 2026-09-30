package com.lifetracker.engine.core.audio

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.File

/**
 * Thread-safe, process-wide Singleton audio manager for Shoka Launcher.
 * Guarantees that at most ONE MediaPlayer instance exists at any time,
 * preventing duplicate/overlapping playback, zombie background audio,
 * or infinite error loops.
 */
object AudioPlayerManager {

    private const val TAG = "AudioPlayerManager"

    private val lock = Any()
    private val mainHandler = Handler(Looper.getMainLooper())

    private var mediaPlayer: MediaPlayer? = null

    @Volatile
    var currentPath: String? = null
        private set

    @Volatile
    var isPlaying: Boolean = false
        private set

    @Volatile
    var isPaused: Boolean = false
        private set

    var onCompletionListener: (() -> Unit)? = null
    var onStateChangedListener: (() -> Unit)? = null

    fun play(path: String, baseDir: File? = null): Boolean {
        synchronized(lock) {
            try {
                // Immediately stop and release existing MediaPlayer
                stopAndReleaseInternal()

                val file = if (path.startsWith("/")) File(path) else File(baseDir, path)
                if (!file.exists() || !file.canRead()) {
                    Log.e(TAG, "Audio file not found or cannot read: ${file.absolutePath}")
                    currentPath = null
                    isPlaying = false
                    isPaused = false
                    notifyStateChanged()
                    return false
                }

                currentPath = file.absolutePath

                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(file.absolutePath)

                    setOnErrorListener { mp, what, extra ->
                        Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra on file $currentPath")
                        synchronized(lock) {
                            try { mp.reset() } catch (_: Exception) {}
                            try { mp.release() } catch (_: Exception) {}
                            if (mediaPlayer == mp) {
                                mediaPlayer = null
                            }
                            this@AudioPlayerManager.isPlaying = false
                            this@AudioPlayerManager.isPaused = false
                        }
                        notifyStateChanged()
                        // CRITICAL: return true so onCompletionListener is NOT invoked on errors!
                        true
                    }

                    setOnCompletionListener {
                        Log.d(TAG, "Playback completed for $currentPath")
                        synchronized(lock) {
                            this@AudioPlayerManager.isPlaying = false
                            this@AudioPlayerManager.isPaused = false
                        }
                        notifyStateChanged()
                        mainHandler.post {
                            onCompletionListener?.invoke()
                        }
                    }

                    prepare()
                    start()
                }

                mediaPlayer = player
                isPlaying = true
                isPaused = false
                Log.d(TAG, "Playback started: ${file.absolutePath}")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to start audio playback for $path", e)
                stopAndReleaseInternal()
                notifyStateChanged()
                return false
            }
        }

        notifyStateChanged()
        return true
    }

    fun pause() {
        synchronized(lock) {
            try {
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.pause()
                    isPlaying = false
                    isPaused = true
                    Log.d(TAG, "Playback paused: $currentPath")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error pausing audio", e)
            }
        }
        notifyStateChanged()
    }

    fun resume() {
        synchronized(lock) {
            try {
                if (mediaPlayer != null && !isPlaying) {
                    mediaPlayer?.start()
                    isPlaying = true
                    isPaused = false
                    Log.d(TAG, "Playback resumed: $currentPath")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error resuming audio", e)
            }
        }
        notifyStateChanged()
    }

    fun stop() {
        synchronized(lock) {
            stopAndReleaseInternal()
            Log.d(TAG, "Playback stopped")
        }
        notifyStateChanged()
    }

    fun seekTo(positionMs: Int) {
        synchronized(lock) {
            try {
                mediaPlayer?.seekTo(positionMs)
            } catch (e: Exception) {
                Log.e(TAG, "Error seeking audio to $positionMs", e)
            }
        }
    }

    fun getPosition(): Int {
        return synchronized(lock) {
            try {
                if (mediaPlayer != null && (isPlaying || isPaused)) {
                    mediaPlayer?.currentPosition ?: 0
                } else {
                    0
                }
            } catch (_: Exception) {
                0
            }
        }
    }

    fun getDuration(): Int {
        return synchronized(lock) {
            try {
                if (mediaPlayer != null && (isPlaying || isPaused)) {
                    mediaPlayer?.duration ?: 0
                } else {
                    0
                }
            } catch (_: Exception) {
                0
            }
        }
    }

    fun isActuallyPlaying(): Boolean {
        return synchronized(lock) {
            try {
                mediaPlayer?.isPlaying == true || isPlaying
            } catch (_: Exception) {
                false
            }
        }
    }

    private fun stopAndReleaseInternal() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.reset()
                player.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Exception during stopAndReleaseInternal", e)
        } finally {
            mediaPlayer = null
            isPlaying = false
            isPaused = false
        }
    }

    private fun notifyStateChanged() {
        mainHandler.post {
            onStateChangedListener?.invoke()
        }
    }
}
