package com.lifetracker.engine.core.module

import com.lifetracker.engine.core.model.BlockUiModel
import kotlinx.coroutines.flow.StateFlow

/**
 * Contrato base para qualquer Módulo (plugin) do Modular Life Tracker Engine.
 * Segue a filosofia do Neovim: registra comandos e renderiza widgets/blocos.
 */
interface EngineModule {
    /**
     * Identificador único (ex: "water", "gym", "github")
     */
    val id: String

    /**
     * Nome amigável de exibição (ex: "Water Tracker")
     */
    val name: String

    /**
     * Prefixo curto usado no Terminal CLI para acionar este módulo (ex: "a", "g", "gh")
     */
    val commandPrefix: String

    /**
     * Breve ajuda de sintaxe para o comando `help` (ex: "a <ml> - Adiciona ingestão de água")
     */
    val helpText: String

    /**
     * Fluxo reativo do Bloco a ser renderizado na tela inicial.
     * Retorna null caso o módulo não queira exibir nada no momento.
     */
    val blockFlow: StateFlow<BlockUiModel?>

    /**
     * Processa a string de argumentos enviada pelo Terminal CLI.
     * Exemplo: se o usuário digitar "a 500", [args] conterá "500".
     */
    suspend fun executeCommand(args: String): CommandResult
}
