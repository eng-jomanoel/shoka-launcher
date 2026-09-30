package com.lifetracker.engine.core.module

/**
 * Resposta retornada quando um Módulo executa um comando do CLI
 */
sealed interface CommandResult {
    data class Success(val message: String) : CommandResult
    data class Error(val reason: String) : CommandResult
    data object Ignored : CommandResult
}
