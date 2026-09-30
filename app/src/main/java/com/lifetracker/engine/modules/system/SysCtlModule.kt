package com.lifetracker.engine.modules.system

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.media.AudioManager
import com.lifetracker.engine.core.model.BlockUiModel
import com.lifetracker.engine.core.module.CommandResult
import com.lifetracker.engine.core.module.CommandSuggestion
import com.lifetracker.engine.core.module.EngineModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SysCtlModule(
    private val context: Context,
    private val onBrightnessChange: (Float) -> Unit,
    private val onUnpin: () -> Unit = {},
    private val onReload: () -> Int = { 0 }
) : EngineModule {

    override val id: String = "sysctl"
    override val name: String = "System"
    override val commandPrefix: String = "sys"
    override val helpText: String = "sys <vol | bri> <0-100> | sys reload | sys unpin | sys remove-admin"

    private val _blockFlow = MutableStateFlow<BlockUiModel?>(null)
    override val blockFlow: StateFlow<BlockUiModel?> = _blockFlow.asStateFlow()

    override fun getSuggestions(args: String): List<CommandSuggestion> {
        val query = args.trim().lowercase()
        val all = listOf(
            CommandSuggestion("sys reload", "sys reload", true),
            CommandSuggestion("sys unpin", "sys unpin", true),
            CommandSuggestion("sys vol ", "sys vol <0-100>", false),
            CommandSuggestion("sys bri ", "sys bri <0-100>", false),
            CommandSuggestion("sys remove-admin", "sys remove-admin", true)
        )
        return if (query.isEmpty()) all else all.filter { it.command.contains(query) }
    }

    override suspend fun executeCommand(args: String): CommandResult {
        val tokens = args.trim().lowercase().split(" ")
        
        if (tokens.isEmpty() || tokens[0] == "-h" || tokens[0] == "--help") {
            return CommandResult.Success(
                "MÓDULO SYSTEM (sys)\n" +
                "  sys vol <0-100>    : Define o volume de mídia\n" +
                "  sys bri <0-100>    : Define o brilho da tela (neste app)\n" +
                "  sys reload         : Recarrega todos os módulos Lua da pasta Documents\n" +
                "  sys unpin          : Desativa temporariamente a trava de tela\n" +
                "  sys remove-admin   : Remove privilégios de Device Owner\n" +
                "  sys -h             : Mostra esta ajuda"
            )
        }

        if (tokens[0] == "reload") {
            val count = onReload()
            return CommandResult.Success("Módulos Lua recarregados com sucesso ($count carregados).")
        }

        if (tokens[0] == "unpin") {
            onUnpin()
            return CommandResult.Success("Modo Kiosk destravado temporariamente.")
        }

        if (tokens[0] == "remove-admin") {
            return try {
                val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                if (dpm.isDeviceOwnerApp(context.packageName)) {
                    dpm.clearDeviceOwnerApp(context.packageName)
                    onUnpin()
                    CommandResult.Success("Device Owner removido com sucesso!")
                } else {
                    CommandResult.Error("O app não está configurado como Device Owner.")
                }
            } catch (e: Exception) {
                CommandResult.Error("Falha ao remover Device Owner: ${e.message}")
            }
        }

        if (tokens.size < 2) {
            return CommandResult.Error("Uso incompleto. Digite 'sys -h' para ajuda.")
        }

        val action = tokens[0]
        val value = tokens[1].toIntOrNull()

        if (value == null || value !in 0..100) {
            return CommandResult.Error("O valor deve ser entre 0 e 100.")
        }

        return when (action) {
            "vol", "volume" -> {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val targetVolume = (value * maxVolume) / 100
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, AudioManager.FLAG_SHOW_UI)
                CommandResult.Success("Volume definido para $value%")
            }
            "bri", "brightness", "luz", "brilho" -> {
                val brightnessFloat = value / 100f
                onBrightnessChange(brightnessFloat)
                CommandResult.Success("Brilho definido para $value%")
            }
            else -> CommandResult.Error("Comando desconhecido. Use 'vol', 'bri', 'unpin' ou 'remove-admin'.")
        }
    }
}
