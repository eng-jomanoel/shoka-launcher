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

    // Atualiza o banner sempre que o app volta para a tela (ex: ao voltar das configurações)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isDefault = isDefaultLauncher(context)
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
        }
    }

    val activeBlocks by moduleRegistry.activeBlocksFlow.collectAsState()
    val isEditMode by moduleRegistry.isEditMode.collectAsState()

    val executeCommand: (String) -> Unit = { command ->
        coroutineScope.launch {
            val result = moduleRegistry.dispatch(command)
            lastCommandResult = result
            currentInput = ""
        }
    }

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
            onMoveUp = { moduleRegistry.moveUp(it) },
            onMoveDown = { moduleRegistry.moveDown(it) },
            onHide = { moduleRegistry.hideModule(it) },
            onExitEditMode = { moduleRegistry.toggleEditMode() },
            modifier = Modifier.weight(1f)
        )

        // Banner movido para a parte de baixo (acima do terminal CLI)
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

        CliZone(
            lastResult = lastCommandResult,
            currentInput = currentInput,
            onInputChange = { currentInput = it },
            onExecuteCommand = executeCommand
        )
    }
}

private fun isDefaultLauncher(context: Context): Boolean {
    val intent = Intent(Intent.ACTION_MAIN)
    intent.addCategory(Intent.CATEGORY_HOME)
    val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
    return resolveInfo?.activityInfo?.packageName == context.packageName
}
