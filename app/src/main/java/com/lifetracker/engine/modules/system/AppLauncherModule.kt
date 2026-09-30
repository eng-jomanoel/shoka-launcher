package com.lifetracker.engine.modules.system

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import com.lifetracker.engine.core.model.BlockUiModel
import com.lifetracker.engine.core.module.CommandResult
import com.lifetracker.engine.core.module.CommandSuggestion
import com.lifetracker.engine.core.module.EngineModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Módulo nativo do Launcher responsável por encontrar e abrir outros aplicativos.
 * Prefixo: "o" (open)
 * Exemplo: "o cam" (abre a câmera), "o config" (abre configurações)
 */
class AppLauncherModule(
    private val context: Context,
    private val onLaunchApp: (Intent) -> Unit
) : EngineModule {

    override val id: String = "app_launcher"
    override val name: String get() = context.getString(com.lifetracker.engine.R.string.app_launcher_name)
    override val commandPrefix: String = "o"
    override val helpText: String get() = context.getString(com.lifetracker.engine.R.string.app_launcher_help_text)

    // Este módulo opera apenas de forma invisível via CLI, não exibe bloco na tela inicial
    private val _blockFlow = MutableStateFlow<BlockUiModel?>(null)
    override val blockFlow: StateFlow<BlockUiModel?> = _blockFlow.asStateFlow()

    private var cachedApps = emptyList<AppItem>()
    fun getInstalledAppNames(): List<String> = cachedApps.map { it.label }

    init {
        // Carrega a lista de apps assincronamente ao iniciar a Engine
        Thread {
            cachedApps = loadInstalledApps()
        }.start()
    }

    override fun getSuggestions(args: String): List<CommandSuggestion> {
        val query = args.trim().lowercase()
        val apps = if (cachedApps.isEmpty()) loadInstalledApps().also { cachedApps = it } else cachedApps
        val matches = if (query.isEmpty()) {
            apps.take(8)
        } else {
            apps.filter { it.label.lowercase().contains(query) }.take(8)
        }
        return matches.map { app ->
            CommandSuggestion(
                command = "o ${app.label}",
                displayText = "o ${app.label}",
                isExecutable = true
            )
        }
    }

    override suspend fun executeCommand(args: String): CommandResult {
        val query = args.trim().lowercase()
        
        if (query == "-h" || query == "--help") {
            return CommandResult.Success(context.getString(com.lifetracker.engine.R.string.app_launcher_help_full))
        }

        if (query == "-l" || query == "--list") {
            if (cachedApps.isEmpty()) {
                cachedApps = withContext(Dispatchers.IO) { loadInstalledApps() }
            }
            val appNames = cachedApps.joinToString(", ") { it.label }
            return CommandResult.Success(context.getString(com.lifetracker.engine.R.string.app_installed_apps, appNames))
        }

        if (query.isEmpty()) {
            return CommandResult.Error(context.getString(com.lifetracker.engine.R.string.app_usage))
        }

        if (cachedApps.isEmpty()) {
            cachedApps = withContext(Dispatchers.IO) { loadInstalledApps() }
        }

        // Tenta encontrar um app cujo nome contenha o termo digitado
        val matches = cachedApps.filter { it.label.lowercase().contains(query) }

        return when {
            matches.isEmpty() -> CommandResult.Error(context.getString(com.lifetracker.engine.R.string.app_not_found, query))
            matches.size == 1 -> launchApp(matches.first())
            else -> {
                // Se o termo bater exatamente com o nome de um deles (ex: "camera" vs "camera dupla")
                val exactMatch = matches.find { it.label.lowercase() == query }
                if (exactMatch != null) {
                    launchApp(exactMatch)
                } else {
                    val nomes = matches.take(3).joinToString(", ") { it.label }
                    CommandResult.Error(context.getString(com.lifetracker.engine.R.string.app_multiple, nomes))
                }
            }
        }
    }

    private fun launchApp(app: AppItem): CommandResult {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                onLaunchApp(intent)
                CommandResult.Success(context.getString(com.lifetracker.engine.R.string.app_opening, app.label))
            } else {
                CommandResult.Error(context.getString(com.lifetracker.engine.R.string.app_cannot_open, app.label))
            }
        } catch (e: Exception) {
            CommandResult.Error(context.getString(com.lifetracker.engine.R.string.app_error))
        }
    }

    private fun loadInstalledApps(): List<AppItem> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)

        val resolvedInfos: List<ResolveInfo> = pm.queryIntentActivities(mainIntent, 0)
        return resolvedInfos.map {
            AppItem(
                label = it.loadLabel(pm).toString(),
                packageName = it.activityInfo.packageName
            )
        }.sortedBy { it.label }
    }

    private data class AppItem(
        val label: String,
        val packageName: String
    )
}
