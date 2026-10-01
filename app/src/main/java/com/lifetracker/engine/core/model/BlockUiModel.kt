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
        val showExerciseList: Boolean = false,
        val days: List<WorkoutDayItem> = emptyList(),
        val files: List<WorkoutFileItem> = emptyList(),
        val showFileList: Boolean = false,
        val decWeightCmd: String = "g w-",
        val incWeightCmd: String = "g w+",
        val decRepsCmd: String = "g r-",
        val incRepsCmd: String = "g r+"
    ) : BlockUiModel

    /**
     * Bloco de Anotações e Tarefas / Checklist
     */
    data class Notes(
        override val moduleId: String,
        override val title: String,
        val subtitle: String? = null,
        val isOpen: Boolean = true,
        val content: String = "",
        val items: List<NoteItem> = emptyList(),
        val actions: List<BlockAction> = emptyList(),
        val inputHint: String? = null,
        val inputCommand: String? = null
    ) : BlockUiModel

    data class NoteItem(
        val id: Int,
        val text: String,
        val isChecklist: Boolean = false,
        val isChecked: Boolean = false,
        val toggleCommand: String? = null,
        val deleteCommand: String? = null
    )

    /**
     * Bloco de Calendário/Agenda
     */
    data class Agenda(
        override val moduleId: String,
        override val title: String,
        val subtitle: String? = null,
        val isOpen: Boolean = true,
        val monthLabel: String = "",
        val days: List<CalendarDay> = emptyList(),
        val events: List<AgendaEvent> = emptyList(),
        val tasks: List<NoteItem> = emptyList(), // Reusa NoteItem para tarefas
        val actions: List<BlockAction> = emptyList(),
        val inputHint: String? = null,
        val inputCommand: String? = null
    ) : BlockUiModel

    data class CalendarDay(
        val dayNumber: Int,
        val isToday: Boolean = false,
        val isSelected: Boolean = false,
        val hasEvents: Boolean = false,
        val hasTasks: Boolean = false,
        val dayOfWeekLabel: String = "",
        val fullDateStr: String = "",
        val commandToExecute: String? = null
    )

    data class AgendaEvent(
        val id: String,
        val title: String,
        val timeLabel: String,
        val isAllDay: Boolean = false,
        val location: String? = null,
        val colorHex: String? = null,
        val commandToExecute: String? = null,
        val deleteCommand: String? = null
    )

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

    data class WorkoutDayItem(
        val dayId: String,
        val label: String,
        val isCurrent: Boolean,
        val isToday: Boolean = false,
        val commandToExecute: String
    )

    data class WorkoutFileItem(
        val name: String,
        val isActive: Boolean,
        val commandToExecute: String
    )
}

