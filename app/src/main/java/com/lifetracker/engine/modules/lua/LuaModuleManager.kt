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
        File(docs, "ModularLife").apply { mkdirs() }
    }

    val modulesDir: File by lazy {
        File(rootDir, "modules").apply { mkdirs() }
    }

    val dataDir: File by lazy {
        File(rootDir, "data").apply { mkdirs() }
    }

    private val bridge: LuaEngineBridge by lazy {
        LuaEngineBridge(
            context = context,
            baseDir = rootDir,
            onOpenApp = onOpenApp,
            onMusicComplete = {
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                    moduleRegistry.dispatch("p next")
                }
            },
            onMusicStateChanged = {
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                    activeLuaModules.forEach { mod ->
                        mod.updateUi()
                    }
                }
            }
        )

    }

    private val activeLuaModules = mutableListOf<LuaModule>()

    fun init() {
        loadAllModules()
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
