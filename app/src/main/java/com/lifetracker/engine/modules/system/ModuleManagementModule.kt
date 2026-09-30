package com.lifetracker.engine.modules.system

import com.lifetracker.engine.core.model.BlockUiModel
import com.lifetracker.engine.core.module.CommandResult
import com.lifetracker.engine.core.module.EngineModule
import com.lifetracker.engine.core.module.ModuleRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ModuleManagementModule(
    private val moduleRegistry: ModuleRegistry
) : EngineModule {

    override val id: String = "module_manager"
    override val name: String = "Gerenciador de Módulos"
    override val commandPrefix: String = "mod"
    override val helpText: String = "mod <list | move | up | down | hide | show | edit>"

    private val _blockFlow = MutableStateFlow<BlockUiModel?>(null)
    override val blockFlow: StateFlow<BlockUiModel?> = _blockFlow.asStateFlow()

    override suspend fun executeCommand(args: String): CommandResult {
        val tokens = args.trim().split("\\s+".toRegex())
        val subcmd = if (tokens.isNotEmpty() && tokens[0].isNotBlank()) tokens[0].lowercase() else "list"

        if (subcmd == "-h" || subcmd == "--help") {
            return CommandResult.Success(
                "MÓDULO DE GESTÃO (mod)\n" +
                "  mod              : Lista os módulos e sua ordem na tela\n" +
                "  mod up <id/num>  : Sobe o módulo 1 posição\n" +
                "  mod down <id/num>: Desce o módulo 1 posição\n" +
                "  mod move <id> <pos> : Move para posição específica\n" +
                "  mod hide <id/num>: Oculta o módulo da tela\n" +
                "  mod show <id/num>: Exibe o módulo ocultado\n" +
                "  mod edit         : Alterna o modo visual de setas na Home"
            )
        }

        if (subcmd == "edit") {
            val isNowEdit = moduleRegistry.toggleEditMode()
            return if (isNowEdit) {
                CommandResult.Success("Modo de Edição ATIVADO. Use as setas [▲] [▼] na Home para mover os cards.")
            } else {
                CommandResult.Success("Modo de Edição DESATIVADO.")
            }
        }

        val allModules = moduleRegistry.modules.value
        val order = moduleRegistry.moduleOrder.value
        val hidden = moduleRegistry.hiddenModules.value

        // Ordena a lista atual
        val sorted = allModules.sortedBy { mod ->
            val idx = order.indexOf(mod.id)
            if (idx == -1) 999 else idx
        }

        if (subcmd == "list") {
            val sb = StringBuilder("MÓDULOS NA HOME (Ordem atual):\n")
            sorted.forEachIndexed { index, mod ->
                val status = if (mod.id in hidden) "[OCULTO]" else "[VISÍVEL]"
                val prefix = "[${mod.commandPrefix}]"
                sb.append(String.format("  %d. %-12s %-6s %-18s %s\n", index + 1, mod.id, prefix, mod.name, status))
            }
            sb.append("\nDica: use 'mod up <id>' ou 'mod edit' para mover.")
            return CommandResult.Success(sb.toString())
        }

        if (tokens.size < 2) {
            return CommandResult.Error("Especifique o ID ou o número do módulo. Digite 'mod -h' para ajuda.")
        }

        val targetQuery = tokens[1]
        val targetMod = sorted.getOrNull(targetQuery.toIntOrNull()?.minus(1) ?: -1)
            ?: sorted.find { it.id.equals(targetQuery, ignoreCase = true) || it.name.contains(targetQuery, ignoreCase = true) }

        if (targetMod == null) {
            return CommandResult.Error("Módulo '$targetQuery' não encontrado. Digite 'mod list'.")
        }

        return when (subcmd) {
            "up", "subir" -> {
                if (moduleRegistry.moveUp(targetMod.id)) {
                    CommandResult.Success("Módulo '${targetMod.name}' movido para cima.")
                } else {
                    CommandResult.Error("O módulo já está no topo.")
                }
            }
            "down", "descer" -> {
                if (moduleRegistry.moveDown(targetMod.id)) {
                    CommandResult.Success("Módulo '${targetMod.name}' movido para baixo.")
                } else {
                    CommandResult.Error("O módulo já está no final.")
                }
            }
            "move", "mover" -> {
                val pos = tokens.getOrNull(2)?.toIntOrNull()
                if (pos == null || pos < 1) {
                    CommandResult.Error("Especifique a posição destino. Ex: 'mod move ${targetMod.id} 1'")
                } else {
                    moduleRegistry.moveToPosition(targetMod.id, pos)
                    CommandResult.Success("Módulo '${targetMod.name}' movido para a posição $pos.")
                }
            }
            "hide", "ocultar" -> {
                moduleRegistry.hideModule(targetMod.id)
                CommandResult.Success("Módulo '${targetMod.name}' ocultado da tela inicial.")
            }
            "show", "exibir" -> {
                moduleRegistry.showModule(targetMod.id)
                CommandResult.Success("Módulo '${targetMod.name}' agora está visível na tela inicial.")
            }
            else -> CommandResult.Error("Subcomando '$subcmd' desconhecido. Digite 'mod -h'.")
        }
    }
}
