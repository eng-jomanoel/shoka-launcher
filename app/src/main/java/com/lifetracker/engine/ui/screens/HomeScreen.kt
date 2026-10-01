package com.lifetracker.engine.ui.screens

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.lifetracker.engine.core.module.CommandResult
import com.lifetracker.engine.core.module.ModuleRegistry
import com.lifetracker.engine.ui.components.CliZone
import com.lifetracker.engine.ui.components.FeedZone
import com.lifetracker.engine.ui.components.HeaderZone
import com.lifetracker.engine.ui.theme.EngineTheme
import com.lifetracker.engine.ui.theme.EngineTypography
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    moduleRegistry: ModuleRegistry,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // Compatibilidade com diferentes versões do Compose
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var lastCommandResult by remember { mutableStateOf<CommandResult?>(null) }
    var currentInput by remember { mutableStateOf("") }
    var isDefault by remember { mutableStateOf(isDefaultLauncher(context)) }
    var hasStoragePermission by remember { mutableStateOf(checkStoragePermission()) }
    var clearResultJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    // Atualiza o banner sempre que o app volta para a tela (ex: ao voltar das configurações)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isDefault = isDefaultLauncher(context)
                val granted = checkStoragePermission()
                if (!hasStoragePermission && granted) {
                    try {
                        com.lifetracker.engine.MainActivity.luaManager.reloadAll()
                        com.lifetracker.engine.MainActivity.themeModule.ensureDefaultThemes()
                    } catch (_: Exception) {}
                }
                hasStoragePermission = granted
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    BackHandler(enabled = true) {
        if (currentInput.isNotEmpty()) {
            currentInput = ""
        } else if (lastCommandResult != null) {
            clearResultJob?.cancel()
            lastCommandResult = null
        }
    }

    val activeBlocks by moduleRegistry.activeBlocksFlow.collectAsState()
    val isEditMode by moduleRegistry.isEditMode.collectAsState()

    val executeCommand: (String) -> Unit = remember(moduleRegistry, coroutineScope) {
        { command ->
            coroutineScope.launch {
                clearResultJob?.cancel()
                val trimmed = command.trim()
                if (trimmed.equals("clear", ignoreCase = true) || trimmed.equals("cls", ignoreCase = true)) {
                    lastCommandResult = null
                    currentInput = ""
                } else {
                    val result = moduleRegistry.dispatch(command)
                    lastCommandResult = result
                    currentInput = ""
                    // Faz a mensagem de retorno sumir automaticamente respeitando o tamanho do texto
                    if (result !is CommandResult.Ignored) {
                        val delayMs = when {
                            result is CommandResult.Error -> 8000L
                            result is CommandResult.Success && (result.message.contains("\n") || result.message.length > 80) -> 12000L
                            trimmed.equals("help", ignoreCase = true) -> 12000L
                            else -> 4500L
                        }
                        clearResultJob = coroutineScope.launch {
                            kotlinx.coroutines.delay(delayMs)
                            lastCommandResult = null
                        }
                    }
                }
            }
        }
    }

    val onMoveUp: (String) -> Unit = remember(moduleRegistry) { { moduleRegistry.moveUp(it) } }
    val onMoveDown: (String) -> Unit = remember(moduleRegistry) { { moduleRegistry.moveDown(it) } }
    val onHide: (String) -> Unit = remember(moduleRegistry) { { moduleRegistry.hideModule(it) } }
    val onExitEditMode: () -> Unit = remember(moduleRegistry) { { moduleRegistry.toggleEditMode() } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EngineTheme.colors.background)
            .systemBarsPadding()
            .imePadding()
    ) {
        HeaderZone()

        FeedZone(
            blocks = activeBlocks,
            onActionClick = executeCommand,
            isEditMode = isEditMode,
            onMoveUp = onMoveUp,
            onMoveDown = onMoveDown,
            onHide = onHide,
            onExitEditMode = onExitEditMode,
            modifier = Modifier.weight(1f)
        )

        // Banner de permissão de armazenamento no Android 11+
        if (!hasStoragePermission) {
            Text(
                text = "⚠️ Permissão de Armazenamento necessária para ler plugins e temas (Toque para conceder)",
                color = androidx.compose.ui.graphics.Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(EngineTheme.colors.accentRed)
                    .clickable {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                            try {
                                val uri = android.net.Uri.parse("package:${context.packageName}")
                                context.startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri))
                            } catch (_: Exception) {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                                } catch (_: Exception) {}
                            }
                        }
                    }
                    .padding(vertical = 8.dp),
                style = EngineTypography.labelSmall
            )
        }

        // Banner de launcher padrão
        if (!isDefault) {
            Text(
                text = androidx.compose.ui.res.stringResource(id = com.lifetracker.engine.R.string.set_default_launcher),
                color = EngineTheme.colors.background,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(EngineTheme.colors.accentAmber)
                    .clickable {
                        try {
                            context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
                        } catch (e: Exception) {
                            context.startActivity(Intent(Settings.ACTION_SETTINGS))
                        }
                    }
                    .padding(vertical = 8.dp),
                style = EngineTypography.labelSmall
            )
        }

        val suggestions = remember(currentInput, activeBlocks) {
            if (currentInput.isBlank()) emptyList() else moduleRegistry.getSuggestions(currentInput)
        }

        CliZone(
            lastResult = lastCommandResult,
            currentInput = currentInput,
            onInputChange = { currentInput = it },
            onExecuteCommand = executeCommand,
            suggestions = suggestions,
            onSuggestionClick = { sugg ->
                if (sugg.isExecutable) {
                    executeCommand(sugg.command)
                } else {
                    currentInput = sugg.command
                }
            }
        )
    }
}

private fun isDefaultLauncher(context: Context): Boolean {
    val intent = Intent(Intent.ACTION_MAIN)
    intent.addCategory(Intent.CATEGORY_HOME)
    val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
    return resolveInfo?.activityInfo?.packageName == context.packageName
}

private fun checkStoragePermission(): Boolean {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        android.os.Environment.isExternalStorageManager()
    } else {
        true
    }
}
