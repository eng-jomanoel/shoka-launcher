package com.lifetracker.engine.core.model

import androidx.compose.ui.graphics.Color

/**
 * Representa os tipos de Blocos visuais que qualquer Módulo pode emitir
 * para ser desenhado no Feed Dinâmico da tela inicial.
 */
sealed interface BlockUiModel {
    val moduleId: String
    val title: String

    /**
     * Bloco de Informação/Texto simples
     */
    data class Info(
        override val moduleId: String,
        override val title: String,
        val description: String,
        val tag: String? = null,
        val tagColor: Color = Color.Cyan
    ) : BlockUiModel

    /**
     * Bloco de Progresso com Barra (ex: Água, Calorias, Passos)
     */
    data class Progress(
        override val moduleId: String,
        override val title: String,
        val current: Float,
        val target: Float,
        val unit: String,
        val barColor: Color = Color.Green
    ) : BlockUiModel {
        val percentage: Float
            get() = if (target > 0) (current / target).coerceIn(0f, 1f) else 0f
    }

    /**
     * Bloco Interativo com botões rápidos de ação
     */
    data class Interactive(
        override val moduleId: String,
        override val title: String,
        val status: String,
        val actions: List<BlockAction>
    ) : BlockUiModel

    /**
     * Bloco de Mídia Rico com suporte a foto de capa da playlist,
     * controles de reprodução (Play, Pause, Skip, Back) e lista clicável.
     */
    data class Media(
        override val moduleId: String,
        override val title: String,
        val subtitle: String? = null,
        val coverPath: String? = null,
        val isPlaying: Boolean = false,
        val progress: Float? = null,
        val progressText: String? = null,
        val shuffle: Boolean = false,
        val loopMode: String = "off",
        val actions: List<BlockAction> = emptyList(),
        val items: List<MediaItem> = emptyList()
    ) : BlockUiModel

    /**
     * Bloco de Treino / Academia com suporte a séries, repetições,
     * carga, cronômetro de descanso sincronizado e lista de exercícios.
     */
    data class Workout(
        override val moduleId: String,
        override val title: String,
        val dayName: String,
        val exerciseName: String,
        val currentSet: Int,
        val totalSets: Int,
        val targetReps: String,
        val weight: Float,
        val repsLogged: Int,
        val isTimerActive: Boolean = false,
        val timerRemainingSeconds: Int = 0,
        val timerTotalSeconds: Int = 60,
        val progressText: String? = null,
        val actions: List<BlockAction> = emptyList(),
        val exercises: List<WorkoutExerciseItem> = emptyList(),
        val showExerciseList: Boolean = false
    ) : BlockUiModel

    data class BlockAction(
        val label: String,
        val commandToExecute: String
    )

    data class MediaItem(
        val label: String,
        val sublabel: String? = null,
        val commandToExecute: String,
        val isActive: Boolean = false
    )

    data class WorkoutExerciseItem(
        val index: Int,
        val name: String,
        val info: String,
        val isCompleted: Boolean,
        val isCurrent: Boolean,
        val commandToExecute: String
    )
}
