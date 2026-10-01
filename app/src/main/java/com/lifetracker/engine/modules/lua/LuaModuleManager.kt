package com.lifetracker.engine.modules.lua

import android.content.Context
import android.os.Environment
import com.lifetracker.engine.core.module.ModuleRegistry
import kotlinx.coroutines.launch
import java.io.File

class LuaModuleManager(
    private val context: Context,
    private val moduleRegistry: ModuleRegistry,
    private val onOpenApp: (String) -> Unit = {}
) {
    val rootDir: File by lazy {
        val docs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val shokaDir = File(docs, "Shoka")
        try {
            shokaDir.mkdirs()
        } catch (_: Exception) {}

        val target = if (shokaDir.exists() && shokaDir.canWrite()) {
            shokaDir
        } else {
            val fallback = File(context.filesDir, "Shoka")
            try { fallback.mkdirs() } catch (_: Exception) {}
            fallback
        }

        // Auto-migration: if old ModularLife folder exists, copy files to Shoka
        val oldDir = File(docs, "ModularLife")
        if (oldDir.exists() && oldDir.isDirectory) {
            try {
                oldDir.copyRecursively(target, overwrite = false)
            } catch (_: Exception) {}
        }
        target
    }

    val modulesDir: File by lazy {
        File(rootDir, "modules").apply { try { mkdirs() } catch (_: Exception) {} }
    }

    val dataDir: File by lazy {
        File(rootDir, "data").apply { try { mkdirs() } catch (_: Exception) {} }
    }

    private val bridge: LuaEngineBridge by lazy {
        LuaEngineBridge(
            context = context,
            baseDir = rootDir,
            onOpenApp = onOpenApp,
            onMusicComplete = {
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
                    moduleRegistry.dispatch("p next")
                }
            },
            onMusicStateChanged = {
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
                    activeLuaModules.forEach { mod ->
                        mod.updateUi()
                    }
                }
            }
        )

    }

    private val activeLuaModules = mutableListOf<LuaModule>()

    fun init() {
        deployBundledModules()
        loadAllModules()
    }

    private fun deployBundledModules() {
        try {
            val assetModules = context.assets.list("modules") ?: return
            for (assetName in assetModules) {
                val targetFile = File(modulesDir, assetName)
                if (!targetFile.exists()) {
                    context.assets.open("modules/$assetName").use { input ->
                        targetFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun reloadAll(): Int {
        // Desregistra os módulos Lua antigos
        activeLuaModules.forEach { module ->
            moduleRegistry.unregister(module.id)
        }
        activeLuaModules.clear()

        // Recarrega todos os arquivos da pasta
        return loadAllModules()
    }

    fun loadAllModules(): Int {
        val luaFiles = modulesDir.listFiles { file -> file.extension.lowercase() == "lua" } ?: emptyArray()
        var loadedCount = 0

        for (file in luaFiles) {
            try {
                val luaModule = LuaModule(file, bridge)
                activeLuaModules.add(luaModule)
                moduleRegistry.register(luaModule)
                loadedCount++
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return loadedCount
    }
}
