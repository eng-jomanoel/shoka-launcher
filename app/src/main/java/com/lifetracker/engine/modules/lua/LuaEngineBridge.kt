package com.lifetracker.engine.modules.lua

import android.content.Context
import android.media.MediaPlayer
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LuaEngineBridge(
    private val context: Context,
    private val baseDir: File,
    private val onOpenApp: (String) -> Unit = {},
    var onMusicComplete: () -> Unit = {},
    var onMusicStateChanged: () -> Unit = {}
) {
    private var mediaPlayer: MediaPlayer? = null
    private var currentPlayingPath: String? = null
    @Volatile private var isPlayingState: Boolean = false
    @Volatile private var isPausedState: Boolean = false
    private val mainHandler = Handler(Looper.getMainLooper())


    private val audioExtensions = setOf("mp3", "m4a", "aac", "flac", "wav", "ogg", "opus")
    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp")

    fun inject(globals: Globals) {
        val engine = LuaTable()

        // --- Engine.audio ---
        val audio = LuaTable()
        audio.set("play", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val path = arg.checkjstring()
                playAudio(path)
                return LuaValue.TRUE
            }
        })
        audio.set("pause", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                pauseAudio()
                return LuaValue.TRUE
            }
        })
        audio.set("resume", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                resumeAudio()
                return LuaValue.TRUE
            }
        })
        audio.set("stop", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                stopAudio()
                return LuaValue.TRUE
            }
        })
        audio.set("is_playing", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                val actual = try { mediaPlayer?.isPlaying == true } catch (_: Exception) { false }
                val playing = actual || (isPlayingState && !isPausedState)
                return LuaValue.valueOf(playing)
            }
        })
        audio.set("get_position", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                return try {
                    LuaValue.valueOf(mediaPlayer?.currentPosition ?: 0)
                } catch (_: Exception) {
                    LuaValue.valueOf(0)
                }
            }
        })
        audio.set("get_duration", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                return try {
                    LuaValue.valueOf(mediaPlayer?.duration ?: 0)
                } catch (_: Exception) {
                    LuaValue.valueOf(0)
                }
            }
        })

        audio.set("seek", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val ms = arg.checkint()
                mediaPlayer?.seekTo(ms)
                return LuaValue.TRUE
            }
        })
        audio.set("current_path", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                return if (currentPlayingPath != null) LuaValue.valueOf(currentPlayingPath) else LuaValue.NIL
            }
        })
        engine.set("audio", audio)

        // --- Engine.music ---
        val music = LuaTable()
        val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)

        music.set("get_dir", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                return LuaValue.valueOf(musicDir.absolutePath)
            }
        })

        music.set("scan", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                return scanMusicDirectory(musicDir)
            }
        })
        engine.set("music", music)

        // --- Engine.files ---
        val files = LuaTable()
        val dataDir = File(baseDir, "data").apply { mkdirs() }

        files.set("write", object : TwoArgFunction() {
            override fun call(fileName: LuaValue, content: LuaValue): LuaValue {
                val name = fileName.checkjstring()
                val text = content.checkjstring()
                val file = if (name.startsWith("/")) File(name) else File(dataDir, name)
                file.parentFile?.mkdirs()
                file.writeText(text)
                return LuaValue.TRUE
            }
        })

        files.set("append", object : TwoArgFunction() {
            override fun call(fileName: LuaValue, content: LuaValue): LuaValue {
                val name = fileName.checkjstring()
                val text = content.checkjstring()
                val file = if (name.startsWith("/")) File(name) else File(dataDir, name)
                file.parentFile?.mkdirs()
                file.appendText(text)
                return LuaValue.TRUE
            }
        })

        files.set("read", object : OneArgFunction() {
            override fun call(fileName: LuaValue): LuaValue {
                val name = fileName.checkjstring()
                val file = if (name.startsWith("/")) File(name) else File(dataDir, name)
                return if (file.exists()) {
                    LuaValue.valueOf(file.readText())
                } else {
                    LuaValue.NIL
                }
            }
        })

        files.set("exists", object : OneArgFunction() {
            override fun call(fileName: LuaValue): LuaValue {
                val name = fileName.checkjstring()
                val file = if (name.startsWith("/")) File(name) else File(dataDir, name)
                return LuaValue.valueOf(file.exists())
            }
        })

        files.set("list", object : OneArgFunction() {
            override fun call(dirArg: LuaValue): LuaValue {
                val sub = if (dirArg.isnil()) "" else dirArg.checkjstring()
                val dir = if (sub.startsWith("/")) File(sub) else File(dataDir, sub)
                val table = LuaTable()
                dir.listFiles()?.forEachIndexed { index, file ->
                    table.set(index + 1, LuaValue.valueOf(file.name))
                }
                return table
            }
        })
        engine.set("files", files)

        // --- Engine.system ---
        val system = LuaTable()
        system.set("toast", object : OneArgFunction() {
            override fun call(msg: LuaValue): LuaValue {
                val message = msg.checkjstring()
                mainHandler.post {
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
                return LuaValue.TRUE
            }
        })
        system.set("open_app", object : OneArgFunction() {
            override fun call(appQuery: LuaValue): LuaValue {
                val query = appQuery.checkjstring()
                onOpenApp(query)
                return LuaValue.TRUE
            }
        })
        engine.set("system", system)

        // --- Engine.time ---
        val time = LuaTable()
        time.set("now", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                return LuaValue.valueOf(System.currentTimeMillis().toDouble())
            }
        })
        time.set("date", object : OneArgFunction() {
            override fun call(formatArg: LuaValue): LuaValue {
                val pattern = if (formatArg.isnil()) "yyyy-MM-dd HH:mm:ss" else formatArg.checkjstring()
                val sdf = SimpleDateFormat(pattern, Locale.getDefault())
                return LuaValue.valueOf(sdf.format(Date()))
            }
        })
        engine.set("time", time)

        globals.set("Engine", engine)
    }

    private fun scanMusicDirectory(musicDir: File): LuaTable {
        val rootTable = LuaTable()
        val playlistsTable = LuaTable()
        var playlistIndex = 1

        if (musicDir.exists() && musicDir.isDirectory) {
            // 1. Músicas avulsas na raiz de /sdcard/Music
            val rootAudioFiles = musicDir.listFiles { f ->
                f.isFile && audioExtensions.contains(f.extension.lowercase())
            }?.sortedBy { it.name } ?: emptyList()

            val rootCoverFile = musicDir.listFiles { f ->
                f.isFile && imageExtensions.contains(f.extension.lowercase())
            }?.firstOrNull()

            if (rootAudioFiles.isNotEmpty()) {
                val rootPl = LuaTable()
                rootPl.set("name", "Geral")
                rootPl.set("path", musicDir.absolutePath)
                if (rootCoverFile != null) {
                    rootPl.set("cover", rootCoverFile.absolutePath)
                }

                val songsTable = LuaTable()
                rootAudioFiles.forEachIndexed { sIdx, file ->
                    val song = LuaTable()
                    song.set("title", file.nameWithoutExtension)
                    song.set("filename", file.name)
                    song.set("path", file.absolutePath)
                    songsTable.set(sIdx + 1, song)
                }
                rootPl.set("songs", songsTable)
                playlistsTable.set(playlistIndex++, rootPl)
            }

            // 2. Pastas dentro de /sdcard/Music (Tratadas como Playlists)
            val subdirs = musicDir.listFiles { f -> f.isDirectory && !f.name.startsWith(".") }?.sortedBy { it.name } ?: emptyList()

            for (dir in subdirs) {
                val audioFiles = dir.listFiles { f ->
                    f.isFile && audioExtensions.contains(f.extension.lowercase())
                }?.sortedBy { it.name } ?: emptyList()

                // Procura imagem de capa dentro da pasta da playlist
                val coverFile = dir.listFiles { f ->
                    f.isFile && imageExtensions.contains(f.extension.lowercase())
                }?.sortedBy { f ->
                    // Dá preferência a arquivos com nome 'cover', 'folder', 'art'
                    val n = f.nameWithoutExtension.lowercase()
                    if (n.contains("cover") || n.contains("folder") || n.contains("art")) 0 else 1
                }?.firstOrNull()

                val plTable = LuaTable()
                plTable.set("name", dir.name)
                plTable.set("path", dir.absolutePath)
                if (coverFile != null) {
                    plTable.set("cover", coverFile.absolutePath)
                }

                val songsTable = LuaTable()
                audioFiles.forEachIndexed { sIdx, file ->
                    val song = LuaTable()
                    song.set("title", file.nameWithoutExtension)
                    song.set("filename", file.name)
                    song.set("path", file.absolutePath)
                    songsTable.set(sIdx + 1, song)
                }
                plTable.set("songs", songsTable)

                playlistsTable.set(playlistIndex++, plTable)
            }
        }

        rootTable.set("playlists", playlistsTable)
        return rootTable
    }

    private fun playAudio(path: String) {
        isPlayingState = true
        isPausedState = false
        mainHandler.post {
            try {
                mediaPlayer?.release()
                currentPlayingPath = path
                mediaPlayer = MediaPlayer().apply {
                    val file = if (path.startsWith("/")) File(path) else File(baseDir, path)
                    setDataSource(file.absolutePath)
                    prepare()
                    start()
                    setOnCompletionListener {
                        isPlayingState = false
                        isPausedState = false
                        onMusicStateChanged()
                        onMusicComplete()
                    }
                }
                onMusicStateChanged()
            } catch (e: Exception) {
                isPlayingState = false
                isPausedState = false
                e.printStackTrace()
                Toast.makeText(context, "Erro ao tocar áudio: ${e.message}", Toast.LENGTH_SHORT).show()
                onMusicStateChanged()
            }
        }
    }

    private fun pauseAudio() {
        isPlayingState = false
        isPausedState = true
        mainHandler.post {
            try {
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.pause()
                }
                onMusicStateChanged()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun resumeAudio() {
        isPlayingState = true
        isPausedState = false
        mainHandler.post {
            try {
                if (mediaPlayer?.isPlaying == false) {
                    mediaPlayer?.start()
                }
                onMusicStateChanged()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun stopAudio() {
        isPlayingState = false
        isPausedState = false
        mainHandler.post {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
                currentPlayingPath = null
                onMusicStateChanged()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun release() {
        try {
            mediaPlayer?.release()
            mediaPlayer = null
            currentPlayingPath = null
        } catch (_: Exception) {}
    }
}
