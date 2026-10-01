package com.lifetracker.engine.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lifetracker.engine.core.model.BlockUiModel
import com.lifetracker.engine.ui.theme.EngineTheme
import com.lifetracker.engine.ui.theme.EngineTypography
import java.io.File

/**
 * Zona 2: Meio (Feed Dinâmico)
 * LazyColumn que empilha os Blocos emitidos pelos módulos ativos.
 */
@Composable
fun FeedZone(
    blocks: List<BlockUiModel>,
    onActionClick: (String) -> Unit,
    isEditMode: Boolean = false,
    onMoveUp: (String) -> Unit = {},
    onMoveDown: (String) -> Unit = {},
    onHide: (String) -> Unit = {},
    onExitEditMode: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isEditMode) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(EngineTheme.colors.card, RoundedCornerShape(4.dp))
                        .border(1.dp, EngineTheme.colors.accentGreen, RoundedCornerShape(4.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MODO DE EDIÇÃO ATIVO",
                        style = EngineTypography.labelSmall,
                        color = EngineTheme.colors.accentGreen
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(EngineTheme.colors.cardBorder)
                            .clickable { onExitEditMode() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Salvar e Concluir",
                            style = EngineTypography.labelSmall,
                            color = EngineTheme.colors.textPrimary
                        )
                    }
                }
            }
        }

        itemsIndexed(blocks, key = { _, it -> it.moduleId }) { index, block ->
            val canMoveUp = index > 0
            val canMoveDown = index < blocks.size - 1

            Column(modifier = Modifier.fillMaxWidth()) {
                if (isEditMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp, start = 4.dp, end = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Módulo: ${block.moduleId} (#${index + 1})",
                            style = EngineTypography.labelSmall,
                            color = EngineTheme.colors.accentAmber
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Botão ▲ Sobe
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (canMoveUp) EngineTheme.colors.card else EngineTheme.colors.card.copy(alpha = 0.3f))
                                    .border(1.dp, if (canMoveUp) EngineTheme.colors.accentGreen else EngineTheme.colors.cardBorder, RoundedCornerShape(4.dp))
                                    .then(if (canMoveUp) Modifier.clickable { onMoveUp(block.moduleId) } else Modifier)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "▲ Sobe",
                                    style = EngineTypography.labelSmall,
                                    color = if (canMoveUp) EngineTheme.colors.accentGreen else EngineTheme.colors.textMuted
                                )
                            }

                            // Botão ▼ Desce
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (canMoveDown) EngineTheme.colors.card else EngineTheme.colors.card.copy(alpha = 0.3f))
                                    .border(1.dp, if (canMoveDown) EngineTheme.colors.accentGreen else EngineTheme.colors.cardBorder, RoundedCornerShape(4.dp))
                                    .then(if (canMoveDown) Modifier.clickable { onMoveDown(block.moduleId) } else Modifier)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "▼ Desce",
                                    style = EngineTypography.labelSmall,
                                    color = if (canMoveDown) EngineTheme.colors.accentGreen else EngineTheme.colors.textMuted
                                )
                            }

                            // Botão ✕ Ocultar
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(EngineTheme.colors.card)
                                    .border(1.dp, EngineTheme.colors.accentRed, RoundedCornerShape(4.dp))
                                    .clickable { onHide(block.moduleId) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "✕ Ocultar",
                                    style = EngineTypography.labelSmall,
                                    color = EngineTheme.colors.accentRed
                                )
                            }
                        }
                    }
                }


                when (block) {
                    is BlockUiModel.Info -> InfoCard(block)
                    is BlockUiModel.Progress -> ProgressCard(block)
                    is BlockUiModel.Interactive -> InteractiveCard(block, onActionClick)
                    is BlockUiModel.Media -> MediaCard(block, onActionClick)
                    is BlockUiModel.Workout -> WorkoutCard(block, onActionClick)
                    is BlockUiModel.Diet -> DietCard(block, onActionClick)
                    is BlockUiModel.Notes -> NotesCard(block, onActionClick)
                    is BlockUiModel.Agenda -> AgendaCard(block, onActionClick)
                }
            }
        }
    }
}

@Composable
private fun InfoCard(block: BlockUiModel.Info) {
    CardContainer {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = block.title, style = EngineTypography.titleMedium)
            if (block.tag != null) {
                Text(
                    text = "[${block.tag}]",
                    style = EngineTypography.labelSmall,
                    color = block.tagColor
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = block.description, style = EngineTypography.bodyMedium)
    }
}

@Composable
private fun ProgressCard(block: BlockUiModel.Progress) {
    CardContainer {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = block.title, style = EngineTypography.titleMedium)
            Text(
                text = "${block.current.toInt()} / ${block.target.toInt()} ${block.unit}",
                style = EngineTypography.labelSmall,
                color = EngineTheme.colors.textSecondary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { block.percentage },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = block.barColor,
            trackColor = EngineTheme.colors.cardBorder,
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
private fun InteractiveCard(
    block: BlockUiModel.Interactive,
    onActionClick: (String) -> Unit
) {
    CardContainer {
        Text(text = block.title, style = EngineTypography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = block.status, style = EngineTypography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            block.actions.forEach { action ->
                Text(
                    text = action.label,
                    style = EngineTypography.titleMedium,
                    color = EngineTheme.colors.accentCyan,
                    modifier = Modifier
                        .clickable { onActionClick(action.commandToExecute) }
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun PlayIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(16.dp)) {
        val path = Path().apply {
            moveTo(size.width * 0.22f, size.height * 0.15f)
            lineTo(size.width * 0.88f, size.height * 0.5f)
            lineTo(size.width * 0.22f, size.height * 0.85f)
            close()
        }
        drawPath(path, color = color)
    }
}

@Composable
private fun PauseIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(16.dp)) {
        val barW = size.width * 0.26f
        val barH = size.height * 0.72f
        val top = size.height * 0.14f
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.16f, top),
            size = Size(barW, barH),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.58f, top),
            size = Size(barW, barH),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
    }
}

@Composable
private fun PrevIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(14.dp)) {
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.12f, size.height * 0.2f),
            size = Size(size.width * 0.18f, size.height * 0.6f),
            cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
        )
        val path = Path().apply {
            moveTo(size.width * 0.85f, size.height * 0.2f)
            lineTo(size.width * 0.35f, size.height * 0.5f)
            lineTo(size.width * 0.85f, size.height * 0.8f)
            close()
        }
        drawPath(path, color = color)
    }
}

@Composable
private fun NextIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(14.dp)) {
        val path = Path().apply {
            moveTo(size.width * 0.15f, size.height * 0.2f)
            lineTo(size.width * 0.65f, size.height * 0.5f)
            lineTo(size.width * 0.15f, size.height * 0.8f)
            close()
        }
        drawPath(path, color = color)
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.70f, size.height * 0.2f),
            size = Size(size.width * 0.18f, size.height * 0.6f),
            cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
        )
    }
}

@Composable
private fun ShuffleIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(14.dp)) {
        val w = size.width
        val h = size.height
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
            width = 1.8.dp.toPx(),
            cap = StrokeCap.Round
        )
        val path1 = Path().apply {
            moveTo(w * 0.15f, h * 0.25f)
            cubicTo(w * 0.45f, h * 0.25f, w * 0.55f, h * 0.75f, w * 0.85f, h * 0.75f)
        }
        drawPath(path1, color = color, style = stroke)
        val arrow1 = Path().apply {
            moveTo(w * 0.65f, h * 0.60f)
            lineTo(w * 0.85f, h * 0.75f)
            lineTo(w * 0.65f, h * 0.90f)
        }
        drawPath(arrow1, color = color, style = stroke)

        val path2 = Path().apply {
            moveTo(w * 0.15f, h * 0.75f)
            cubicTo(w * 0.45f, h * 0.75f, w * 0.55f, h * 0.25f, w * 0.85f, h * 0.25f)
        }
        drawPath(path2, color = color, style = stroke)
        val arrow2 = Path().apply {
            moveTo(w * 0.65f, h * 0.10f)
            lineTo(w * 0.85f, h * 0.25f)
            lineTo(w * 0.65f, h * 0.40f)
        }
        drawPath(arrow2, color = color, style = stroke)
    }
}

@Composable
private fun LoopIcon(color: Color, isOne: Boolean = false, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(14.dp)) {
        val w = size.width
        val h = size.height
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
            width = 1.8.dp.toPx(),
            cap = StrokeCap.Round
        )
        val upperPath = Path().apply {
            moveTo(w * 0.25f, h * 0.5f)
            lineTo(w * 0.25f, h * 0.28f)
            cubicTo(w * 0.25f, h * 0.2f, w * 0.75f, h * 0.2f, w * 0.75f, h * 0.28f)
            lineTo(w * 0.75f, h * 0.35f)
        }
        drawPath(upperPath, color = color, style = stroke)
        val arrow1 = Path().apply {
            moveTo(w * 0.62f, h * 0.25f)
            lineTo(w * 0.75f, h * 0.38f)
            lineTo(w * 0.88f, h * 0.25f)
        }
        drawPath(arrow1, color = color, style = stroke)

        val lowerPath = Path().apply {
            moveTo(w * 0.75f, h * 0.5f)
            lineTo(w * 0.75f, h * 0.72f)
            cubicTo(w * 0.75f, h * 0.8f, w * 0.25f, h * 0.8f, w * 0.25f, h * 0.72f)
            lineTo(w * 0.25f, h * 0.65f)
        }
        drawPath(lowerPath, color = color, style = stroke)
        val arrow2 = Path().apply {
            moveTo(w * 0.12f, h * 0.75f)
            lineTo(w * 0.25f, h * 0.62f)
            lineTo(w * 0.38f, h * 0.75f)
        }
        drawPath(arrow2, color = color, style = stroke)

        if (isOne) {
            drawCircle(
                color = color,
                radius = 2.dp.toPx(),
                center = Offset(w * 0.5f, h * 0.5f)
            )
        }
    }
}


@Composable
private fun MediaCard(
    block: BlockUiModel.Media,
    onActionClick: (String) -> Unit
) {
    val coverBitmap = remember(block.coverPath) {
        if (!block.coverPath.isNullOrBlank()) {
            val f = File(block.coverPath)
            if (f.exists()) {
                try {
                    BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap()
                } catch (_: Exception) { null }
            } else null
        } else null
    }

    CardContainer {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (coverBitmap != null) {
                Image(
                    bitmap = coverBitmap,
                    contentDescription = "Cover",
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(14.dp))
            } else {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(EngineTheme.colors.cardBorder),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "♪",
                        style = EngineTypography.headlineMedium,
                        color = EngineTheme.colors.accentGreen
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = block.title,
                    style = EngineTypography.titleMedium,
                    maxLines = 1
                )
                if (!block.subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = block.subtitle,
                        style = EngineTypography.bodyMedium,
                        color = EngineTheme.colors.textSecondary,
                        maxLines = 1
                    )
                }
            }
        }

        if (block.progress != null) {
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { block.progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = EngineTheme.colors.accentGreen,
                trackColor = EngineTheme.colors.cardBorder,
                strokeCap = StrokeCap.Round
            )
            if (!block.progressText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = block.progressText,
                    style = EngineTypography.labelSmall,
                    color = EngineTheme.colors.textMuted
                )
            }
        }

        // Barra de Controles: Ícones Circulares à Esquerda, Abas (Queue/Playlists) à Direita
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Grupo de Controles de Áudio (Shuffle, Prev, Play/Pause, Next, Loop)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle
                val shuffleActive = block.shuffle
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(if (shuffleActive) EngineTheme.colors.accentGreen.copy(alpha = 0.2f) else EngineTheme.colors.cardBorder.copy(alpha = 0.4f))
                        .clickable { onActionClick("p shuf") },
                    contentAlignment = Alignment.Center
                ) {
                    ShuffleIcon(color = if (shuffleActive) EngineTheme.colors.accentGreen else EngineTheme.colors.textMuted)
                }

                // Prev
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(EngineTheme.colors.cardBorder.copy(alpha = 0.4f))
                        .clickable { onActionClick("p prev") },
                    contentAlignment = Alignment.Center
                ) {
                    PrevIcon(color = EngineTheme.colors.textPrimary)
                }

                // Play / Pause
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(EngineTheme.colors.accentGreen)
                        .clickable { onActionClick("p toggle") },
                    contentAlignment = Alignment.Center
                ) {
                    if (block.isPlaying) {
                        PauseIcon(color = EngineTheme.colors.background)
                    } else {
                        PlayIcon(color = EngineTheme.colors.background)
                    }
                }

                // Next
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(EngineTheme.colors.cardBorder.copy(alpha = 0.4f))
                        .clickable { onActionClick("p next") },
                    contentAlignment = Alignment.Center
                ) {
                    NextIcon(color = EngineTheme.colors.textPrimary)
                }

                // Loop
                val loopActive = block.loopMode != "off"
                val isLoopOne = block.loopMode == "one"
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(if (loopActive) EngineTheme.colors.accentGreen.copy(alpha = 0.2f) else EngineTheme.colors.cardBorder.copy(alpha = 0.4f))
                        .clickable { onActionClick("p loop") },
                    contentAlignment = Alignment.Center
                ) {
                    LoopIcon(
                        color = if (loopActive) EngineTheme.colors.accentGreen else EngineTheme.colors.textMuted,
                        isOne = isLoopOne
                    )
                }
            }

            // Grupo de Abas de Navegação (Queue, Playlists, etc.)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                block.actions.filterNot { 
                    it.commandToExecute in setOf("p prev", "p toggle", "p next", "p shuf", "p shuffle", "p loop")
                }.forEach { action ->
                    Text(
                        text = action.label,
                        style = EngineTypography.labelSmall,
                        color = EngineTheme.colors.textSecondary,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier
                            .background(EngineTheme.colors.background, RoundedCornerShape(4.dp))
                            .border(1.dp, EngineTheme.colors.cardBorder, RoundedCornerShape(4.dp))
                            .clickable { onActionClick(action.commandToExecute) }
                            .padding(horizontal = 6.dp, vertical = 5.dp)
                    )
                }
            }
        }

        if (block.items.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                block.items.forEach { item ->
                    val itemColor = if (item.isActive) EngineTheme.colors.accentGreen else EngineTheme.colors.textPrimary
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onActionClick(item.commandToExecute) }
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = (if (item.isActive) "▶ " else "  ") + item.label,
                            style = EngineTypography.bodyMedium,
                            color = itemColor,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        if (!item.sublabel.isNullOrBlank()) {
                            Text(
                                text = item.sublabel,
                                style = EngineTypography.labelSmall,
                                color = EngineTheme.colors.textMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CardContainer(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(EngineTheme.colors.card, RoundedCornerShape(8.dp))
            .border(1.dp, EngineTheme.colors.cardBorder, RoundedCornerShape(8.dp))
            .padding(14.dp)
    ) {
        Column {
            content()
        }
    }
}

@Composable
private fun WorkoutCard(
    block: BlockUiModel.Workout,
    onActionClick: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val weightFocusRequester = remember { FocusRequester() }
    val repsFocusRequester = remember { FocusRequester() }

    var isWeightFocused by remember { mutableStateOf(false) }
    var isRepsFocused by remember { mutableStateOf(false) }

    val formatWeight: (Float) -> String = { w ->
        if (w % 1f == 0f) w.toInt().toString() else w.toString()
    }

    var weightValue by remember(block.exerciseName, block.weight) {
        val initial = formatWeight(block.weight)
        mutableStateOf(TextFieldValue(text = initial, selection = TextRange(initial.length)))
    }

    var repsValue by remember(block.exerciseName, block.repsLogged) {
        val initial = block.repsLogged.toString()
        mutableStateOf(TextFieldValue(text = initial, selection = TextRange(initial.length)))
    }

    val commitValues = {
        val w = weightValue.text.replace(',', '.').toFloatOrNull() ?: block.weight
        val r = repsValue.text.toIntOrNull() ?: block.repsLogged
        if (w != block.weight || r != block.repsLogged) {
            val wStr = formatWeight(w)
            onActionClick("g $wStr $r")
        }
    }

    val handleActionClick: (String) -> Unit = { cmd ->
        if (cmd == "g check" || cmd.startsWith("g check")) {
            val w = weightValue.text.replace(',', '.').toFloatOrNull() ?: block.weight
            val r = repsValue.text.toIntOrNull() ?: block.repsLogged
            val wStr = formatWeight(w)
            onActionClick("g check $wStr $r")
        } else {
            commitValues()
            onActionClick(cmd)
        }
    }

    CardContainer {
        // Header: Day Name + Progress Indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = block.dayName,
                style = EngineTypography.titleMedium,
                color = EngineTheme.colors.accentAmber,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (block.progressText != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "[${block.progressText}]",
                    style = EngineTypography.labelSmall,
                    color = EngineTheme.colors.accentGreen,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        // Day / Routine Selector Chips
        if (block.days.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                block.days.forEach { day ->
                    val isSelected = day.isCurrent
                    val bg = if (isSelected) EngineTheme.colors.accentAmber.copy(alpha = 0.2f) else EngineTheme.colors.card
                    val borderCol = if (isSelected) EngineTheme.colors.accentAmber else EngineTheme.colors.cardBorder
                    val txtCol = if (isSelected) EngineTheme.colors.accentAmber else EngineTheme.colors.textMuted

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(bg)
                            .border(1.dp, borderCol, RoundedCornerShape(4.dp))
                            .clickable { handleActionClick(day.commandToExecute) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = day.label,
                            style = EngineTypography.labelSmall,
                            color = txtCol,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Main Exercise Info
        Text(
            text = block.exerciseName,
            style = EngineTypography.titleMedium,
            color = EngineTheme.colors.textPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Badges: Set info + Target reps
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(EngineTheme.colors.cardBorder, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "SÉRIE ${block.currentSet}/${block.totalSets}",
                    style = EngineTypography.labelSmall,
                    color = EngineTheme.colors.accentCyan
                )
            }

            Box(
                modifier = Modifier
                    .background(EngineTheme.colors.cardBorder, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "META: ${block.targetReps} REPS",
                    style = EngineTypography.labelSmall,
                    color = EngineTheme.colors.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Direct Editable Input Fields for Carga & Reps
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Carga Input (Left Box)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(EngineTheme.colors.background, RoundedCornerShape(6.dp))
                    .border(
                        width = 1.dp,
                        color = if (isWeightFocused) EngineTheme.colors.accentAmber else EngineTheme.colors.cardBorder,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable { weightFocusRequester.requestFocus() }
                    .padding(vertical = 8.dp, horizontal = 10.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CARGA (KG)",
                            style = EngineTypography.labelSmall,
                            color = if (isWeightFocused) EngineTheme.colors.accentAmber else EngineTheme.colors.textMuted
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(EngineTheme.colors.cardBorder)
                                    .clickable { handleActionClick(block.decWeightCmd) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("-", style = EngineTypography.labelSmall, color = EngineTheme.colors.textPrimary)
                            }
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(EngineTheme.colors.cardBorder)
                                    .clickable { handleActionClick(block.incWeightCmd) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("+", style = EngineTypography.labelSmall, color = EngineTheme.colors.textPrimary)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        BasicTextField(
                            value = weightValue,
                            onValueChange = { newVal ->
                                val clean = newVal.text.replace(',', '.').filter { it.isDigit() || it == '.' }
                                if (clean.count { it == '.' } <= 1 && clean.length <= 6) {
                                    weightValue = newVal.copy(text = clean)
                                }
                            },
                            textStyle = EngineTypography.titleMedium.copy(
                                color = if (isWeightFocused) EngineTheme.colors.accentAmber else EngineTheme.colors.textPrimary
                            ),
                            cursorBrush = SolidColor(EngineTheme.colors.accentAmber),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { repsFocusRequester.requestFocus() }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(weightFocusRequester)
                                .onFocusChanged { focusState ->
                                    isWeightFocused = focusState.isFocused
                                    if (focusState.isFocused) {
                                        weightValue = weightValue.copy(
                                            selection = TextRange(0, weightValue.text.length)
                                        )
                                    } else {
                                        commitValues()
                                    }
                                },
                            decorationBox = { innerTextField ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "> ",
                                        style = EngineTypography.titleMedium,
                                        color = if (isWeightFocused) EngineTheme.colors.accentAmber else EngineTheme.colors.textMuted
                                    )
                                    if (weightValue.text.isEmpty()) {
                                        Text(
                                            text = "0",
                                            style = EngineTypography.titleMedium,
                                            color = EngineTheme.colors.textMuted
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                        Text(
                            text = "KG",
                            style = EngineTypography.labelSmall,
                            color = EngineTheme.colors.textMuted
                        )
                    }
                }
            }

            // Reps Input (Right Box)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(EngineTheme.colors.background, RoundedCornerShape(6.dp))
                    .border(
                        width = 1.dp,
                        color = if (isRepsFocused) EngineTheme.colors.accentGreen else EngineTheme.colors.cardBorder,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable { repsFocusRequester.requestFocus() }
                    .padding(vertical = 8.dp, horizontal = 10.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "REPETIÇÕES",
                            style = EngineTypography.labelSmall,
                            color = if (isRepsFocused) EngineTheme.colors.accentGreen else EngineTheme.colors.textMuted
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(EngineTheme.colors.cardBorder)
                                    .clickable { handleActionClick(block.decRepsCmd) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("-", style = EngineTypography.labelSmall, color = EngineTheme.colors.textPrimary)
                            }
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(EngineTheme.colors.cardBorder)
                                    .clickable { handleActionClick(block.incRepsCmd) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("+", style = EngineTypography.labelSmall, color = EngineTheme.colors.textPrimary)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        BasicTextField(
                            value = repsValue,
                            onValueChange = { newVal ->
                                val clean = newVal.text.filter { it.isDigit() }
                                if (clean.length <= 4) {
                                    repsValue = newVal.copy(text = clean)
                                }
                            },
                            textStyle = EngineTypography.titleMedium.copy(
                                color = if (isRepsFocused) EngineTheme.colors.accentGreen else EngineTheme.colors.textPrimary
                            ),
                            cursorBrush = SolidColor(EngineTheme.colors.accentGreen),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    commitValues()
                                }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(repsFocusRequester)
                                .onFocusChanged { focusState ->
                                    isRepsFocused = focusState.isFocused
                                    if (focusState.isFocused) {
                                        repsValue = repsValue.copy(
                                            selection = TextRange(0, repsValue.text.length)
                                        )
                                    } else {
                                        commitValues()
                                    }
                                },
                            decorationBox = { innerTextField ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "> ",
                                        style = EngineTypography.titleMedium,
                                        color = if (isRepsFocused) EngineTheme.colors.accentGreen else EngineTheme.colors.textMuted
                                    )
                                    if (repsValue.text.isEmpty()) {
                                        Text(
                                            text = "0",
                                            style = EngineTypography.titleMedium,
                                            color = EngineTheme.colors.textMuted
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                        Text(
                            text = "REPS",
                            style = EngineTypography.labelSmall,
                            color = EngineTheme.colors.textMuted
                        )
                    }
                }
            }
        }

        // Rest Timer Section (when active)
        if (block.isTimerActive) {
            Spacer(modifier = Modifier.height(10.dp))
            val m = block.timerRemainingSeconds / 60
            val s = block.timerRemainingSeconds % 60
            val timerText = String.format("%02d:%02d", m, s)
            val fraction = if (block.timerTotalSeconds > 0) {
                (block.timerRemainingSeconds.toFloat() / block.timerTotalSeconds.toFloat()).coerceIn(0f, 1f)
            } else 0f

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⏱️ DESCANSO",
                    style = EngineTypography.labelSmall,
                    color = EngineTheme.colors.accentAmber
                )
                Text(
                    text = timerText,
                    style = EngineTypography.titleMedium,
                    color = EngineTheme.colors.accentAmber
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = EngineTheme.colors.accentAmber,
                trackColor = EngineTheme.colors.cardBorder,
                strokeCap = StrokeCap.Round
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Action Buttons
        if (block.actions.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                block.actions.forEach { action ->
                    val isCheckAction = action.label.contains("Check", ignoreCase = true) || action.label.contains("✓")
                    val isSkipAction = action.label.contains("Pular", ignoreCase = true)
                    val btnBg = if (isCheckAction) EngineTheme.colors.accentGreen.copy(alpha = 0.2f)
                                else if (isSkipAction) EngineTheme.colors.accentAmber.copy(alpha = 0.2f)
                                else EngineTheme.colors.cardBorder
                    val btnTextColor = if (isCheckAction) EngineTheme.colors.accentGreen
                                       else if (isSkipAction) EngineTheme.colors.accentAmber
                                       else EngineTheme.colors.textPrimary

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(btnBg)
                            .clickable { handleActionClick(action.commandToExecute) }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = action.label,
                            style = EngineTypography.labelSmall,
                            color = btnTextColor,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        // Expandable Exercises List
        if (block.showExerciseList && block.exercises.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = EngineTheme.colors.cardBorder
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                block.exercises.forEach { ex ->
                    val itemBg = if (ex.isCurrent) EngineTheme.colors.cardBorder.copy(alpha = 0.5f) else Color.Transparent
                    val itemColor = if (ex.isCompleted) EngineTheme.colors.accentGreen
                                    else if (ex.isCurrent) EngineTheme.colors.accentAmber
                                    else EngineTheme.colors.textSecondary

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(itemBg)
                            .clickable { handleActionClick(ex.commandToExecute) }
                            .padding(vertical = 6.dp, horizontal = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val icon = if (ex.isCompleted) "✓ " else if (ex.isCurrent) "▶ " else "  "
                        Text(
                            text = "$icon${ex.index}. ${ex.name}",
                            style = EngineTypography.bodyMedium,
                            color = itemColor,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        if (ex.info.isNotBlank()) {
                            Text(
                                text = ex.info,
                                style = EngineTypography.labelSmall,
                                color = EngineTheme.colors.textMuted
                            )
                        }
                    }
                }
            }
        }

        // Expandable Workout Files List (Fichas Disponíveis)
        if (block.showFileList && block.files.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = EngineTheme.colors.cardBorder
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "FICHAS DISPONÍVEIS (DOCUMENTS/SHOKA/DATA/):",
                style = EngineTypography.labelSmall,
                color = EngineTheme.colors.accentAmber
            )
            Spacer(modifier = Modifier.height(6.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                block.files.forEach { file ->
                    val itemBg = if (file.isActive) EngineTheme.colors.accentAmber.copy(alpha = 0.15f) else Color.Transparent
                    val itemColor = if (file.isActive) EngineTheme.colors.accentAmber else EngineTheme.colors.textPrimary
                    val borderCol = if (file.isActive) EngineTheme.colors.accentAmber else EngineTheme.colors.cardBorder.copy(alpha = 0.5f)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(itemBg)
                            .border(1.dp, borderCol, RoundedCornerShape(4.dp))
                            .clickable { handleActionClick(file.commandToExecute) }
                            .padding(vertical = 8.dp, horizontal = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = (if (file.isActive) "▶ " else "  ") + file.name,
                            style = EngineTypography.bodyMedium,
                            color = itemColor,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        if (file.isActive) {
                            Text(
                                text = "[ATIVA]",
                                style = EngineTypography.labelSmall,
                                color = EngineTheme.colors.accentAmber
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DietCard(
    block: BlockUiModel.Diet,
    onActionClick: (String) -> Unit
) {
    CardContainer {
        Column(modifier = Modifier.fillMaxWidth()) {
            // ── Header ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = block.title,
                        style = EngineTypography.titleMedium,
                        color = EngineTheme.colors.accentGreen
                    )
                    Text(
                        text = "${block.dayName} • ${block.planName}",
                        style = EngineTypography.labelSmall,
                        color = EngineTheme.colors.textSecondary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (block.days.size > 1) {
                        block.days.forEach { day ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (day.isCurrent) EngineTheme.colors.accentGreen.copy(alpha = 0.2f)
                                        else EngineTheme.colors.cardBorder
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (day.isCurrent) EngineTheme.colors.accentGreen else Color.Transparent,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .clickable { onActionClick(day.commandToExecute) }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = day.label,
                                    style = EngineTypography.labelSmall,
                                    color = if (day.isCurrent) EngineTheme.colors.accentGreen else EngineTheme.colors.textMuted
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── Progresso de Calorias e Macros ──
            val calorieProgress = if (block.caloriesTarget > 0) {
                (block.caloriesConsumed.toFloat() / block.caloriesTarget.toFloat()).coerceIn(0f, 1f)
            } else 0f
            val caloriePercent = (calorieProgress * 100).toInt()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CALORIAS",
                    style = EngineTypography.labelSmall,
                    color = EngineTheme.colors.textMuted
                )
                Text(
                    text = "${block.caloriesConsumed} / ${block.caloriesTarget} kcal ($caloriePercent%)",
                    style = EngineTypography.labelSmall,
                    color = if (block.caloriesConsumed >= block.caloriesTarget) EngineTheme.colors.accentGreen else EngineTheme.colors.textPrimary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Barra de Progresso de Calorias
            LinearProgressIndicator(
                progress = { calorieProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = EngineTheme.colors.accentGreen,
                trackColor = EngineTheme.colors.cardBorder
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Badges de Macros (Proteína, Carbo, Gordura)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Proteína
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(EngineTheme.colors.cardBorder, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "P: ${block.proteinConsumed}/${block.proteinTarget}g",
                        style = EngineTypography.labelSmall,
                        color = EngineTheme.colors.accentCyan
                    )
                }

                // Carboidrato
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(EngineTheme.colors.cardBorder, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "C: ${block.carbsConsumed}/${block.carbsTarget}g",
                        style = EngineTypography.labelSmall,
                        color = EngineTheme.colors.accentAmber
                    )
                }

                // Gordura
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(EngineTheme.colors.cardBorder, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "G: ${block.fatsConsumed}/${block.fatsTarget}g",
                        style = EngineTypography.labelSmall,
                        color = EngineTheme.colors.accentRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Água
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Água: ${block.waterConsumedMl}/${block.waterTargetMl} ml",
                    style = EngineTypography.labelSmall,
                    color = EngineTheme.colors.accentCyan
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(EngineTheme.colors.cardBorder)
                            .clickable { onActionClick(block.addWaterCommand250) }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(text = "+250ml", style = EngineTypography.labelSmall, color = EngineTheme.colors.textPrimary)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(EngineTheme.colors.cardBorder)
                            .clickable { onActionClick(block.addWaterCommand500) }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(text = "+500ml", style = EngineTypography.labelSmall, color = EngineTheme.colors.textPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── Refeição em Destaque ──
            if (block.currentMealName.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(EngineTheme.colors.background, RoundedCornerShape(6.dp))
                        .border(
                            width = 1.dp,
                            color = if (block.currentMealIsCompleted) EngineTheme.colors.accentGreen else EngineTheme.colors.cardBorder,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(10.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${block.currentMealIndex}/${block.totalMeals} • ${block.currentMealName.uppercase()}",
                                style = EngineTypography.titleMedium,
                                color = if (block.currentMealIsCompleted) EngineTheme.colors.accentGreen else EngineTheme.colors.textPrimary
                            )
                            if (block.currentMealTime.isNotBlank()) {
                                Text(
                                    text = block.currentMealTime,
                                    style = EngineTypography.labelSmall,
                                    color = EngineTheme.colors.accentAmber
                                )
                            }
                        }

                        if (block.currentMealCalories > 0 || block.currentMealMacros.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${if (block.currentMealCalories > 0) "${block.currentMealCalories} kcal • " else ""}${block.currentMealMacros}",
                                style = EngineTypography.labelSmall,
                                color = EngineTheme.colors.textSecondary
                            )
                        }

                        if (!block.currentMealNotes.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Nota: ${block.currentMealNotes}",
                                style = EngineTypography.labelSmall,
                                color = EngineTheme.colors.accentCyan.copy(alpha = 0.8f)
                            )
                        }

                        if (block.currentMealItems.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                block.currentMealItems.forEach { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "• ${item.name}",
                                            style = EngineTypography.bodyMedium,
                                            color = EngineTheme.colors.textPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (item.amount.isNotBlank()) {
                                            Text(
                                                text = item.amount,
                                                style = EngineTypography.bodyMedium,
                                                color = EngineTheme.colors.textMuted
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Botões de Ação da Refeição
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Navegação
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (block.prevMealCommand != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(EngineTheme.colors.cardBorder)
                                            .clickable { onActionClick(block.prevMealCommand) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("<", style = EngineTypography.labelSmall, color = EngineTheme.colors.textPrimary)
                                    }
                                }
                                if (block.nextMealCommand != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(EngineTheme.colors.cardBorder)
                                            .clickable { onActionClick(block.nextMealCommand) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(">", style = EngineTypography.labelSmall, color = EngineTheme.colors.textPrimary)
                                    }
                                }
                            }

                            // Botão de Concluir Refeição
                            if (block.checkMealCommand != null) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (block.currentMealIsCompleted) EngineTheme.colors.accentGreen.copy(alpha = 0.2f)
                                            else EngineTheme.colors.cardBorder
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (block.currentMealIsCompleted) EngineTheme.colors.accentGreen else Color.Transparent,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .clickable { onActionClick(block.checkMealCommand) }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = if (block.currentMealIsCompleted) "✓ Concluída" else "Concluir Refeição",
                                        style = EngineTypography.labelSmall,
                                        color = if (block.currentMealIsCompleted) EngineTheme.colors.accentGreen else EngineTheme.colors.accentCyan
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Lista de Refeições do Dia ──
            if (block.showMealList && block.meals.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "REFEIÇÕES DO DIA",
                    style = EngineTypography.labelSmall,
                    color = EngineTheme.colors.textMuted
                )
                Spacer(modifier = Modifier.height(4.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    block.meals.forEach { meal ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (meal.isCurrent) EngineTheme.colors.cardBorder.copy(alpha = 0.5f)
                                    else Color.Transparent
                                )
                                .clickable { onActionClick(meal.commandToExecute) }
                                .padding(vertical = 4.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (meal.isCompleted) "✓ " else "○ ",
                                style = EngineTypography.labelSmall,
                                color = if (meal.isCompleted) EngineTheme.colors.accentGreen else EngineTheme.colors.textMuted
                            )
                            Text(
                                text = meal.name,
                                style = EngineTypography.bodyMedium,
                                color = if (meal.isCompleted) EngineTheme.colors.textMuted else EngineTheme.colors.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            if (meal.time.isNotBlank()) {
                                Text(
                                    text = meal.time,
                                    style = EngineTypography.labelSmall,
                                    color = EngineTheme.colors.textMuted
                                )
                            }
                            if (meal.calories > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${meal.calories} kcal",
                                    style = EngineTypography.labelSmall,
                                    color = EngineTheme.colors.accentAmber
                                )
                            }
                        }
                    }
                }
            }

            // ── Lista de Arquivos de Dieta ──
            if (block.showFileList && block.files.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "PLANOS DE DIETA DISPONÍVEIS",
                    style = EngineTypography.labelSmall,
                    color = EngineTheme.colors.textMuted
                )
                Spacer(modifier = Modifier.height(4.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    block.files.forEach { file ->
                        val itemColor = if (file.isActive) EngineTheme.colors.accentGreen else EngineTheme.colors.textSecondary
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (file.isActive) EngineTheme.colors.cardBorder.copy(alpha = 0.5f)
                                    else Color.Transparent
                                )
                                .clickable { onActionClick(file.commandToExecute) }
                                .padding(vertical = 4.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = (if (file.isActive) "▶ " else "  ") + file.name,
                                style = EngineTypography.bodyMedium,
                                color = itemColor,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            if (file.isActive) {
                                Text(
                                    text = "[ATIVA]",
                                    style = EngineTypography.labelSmall,
                                    color = EngineTheme.colors.accentGreen
                                )
                            }
                        }
                    }
                }
            }

            // ── Actions ──
            if (block.actions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    block.actions.forEach { action ->
                        Text(
                            text = action.label,
                            style = EngineTypography.titleMedium,
                            color = EngineTheme.colors.accentCyan,
                            modifier = Modifier
                                .clickable { onActionClick(action.commandToExecute) }
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotesCard(
    block: BlockUiModel.Notes,
    onActionClick: (String) -> Unit
) {
    var textValue by remember(block.content) { mutableStateOf(TextFieldValue(block.content)) }
    var isFocused by remember { mutableStateOf(false) }
    var hasUnsavedChanges by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    fun commitSave() {
        if (hasUnsavedChanges) {
            onActionClick("n set ${textValue.text}")
            hasUnsavedChanges = false
        }
    }

    CardContainer {
        Column(modifier = Modifier.fillMaxWidth()) {
            // ── Header ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = block.title, style = EngineTypography.titleMedium)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasUnsavedChanges) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(EngineTheme.colors.accentGreen.copy(alpha = 0.15f))
                                .clickable {
                                    commitSave()
                                    focusManager.clearFocus()
                                }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "Salvar",
                                style = EngineTypography.labelSmall,
                                color = EngineTheme.colors.accentGreen
                            )
                        }
                    }
                    if (block.content.isNotBlank() || textValue.text.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    textValue = TextFieldValue("")
                                    hasUnsavedChanges = false
                                    focusManager.clearFocus()
                                    onActionClick("n clear")
                                }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "Limpar",
                                style = EngineTypography.labelSmall,
                                color = EngineTheme.colors.textMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Bloco de Notas (Área de Texto Multilinha) ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 90.dp, max = 220.dp)
                    .background(EngineTheme.colors.background, RoundedCornerShape(6.dp))
                    .border(
                        width = 1.dp,
                        color = if (isFocused) EngineTheme.colors.accentCyan else EngineTheme.colors.cardBorder,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(10.dp)
            ) {
                BasicTextField(
                    value = textValue,
                    onValueChange = {
                        textValue = it
                        hasUnsavedChanges = (it.text != block.content)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .onFocusChanged { focusState ->
                            isFocused = focusState.isFocused
                            if (!focusState.isFocused && hasUnsavedChanges) {
                                commitSave()
                            }
                        },
                    textStyle = EngineTypography.bodyMedium.copy(color = EngineTheme.colors.textPrimary),
                    cursorBrush = SolidColor(EngineTheme.colors.accentCyan),
                    decorationBox = { innerTextField ->
                        if (textValue.text.isEmpty()) {
                            Text(
                                text = "Toque para anotar...",
                                style = EngineTypography.bodyMedium,
                                color = EngineTheme.colors.textMuted
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }
    }
}

@Composable
private fun AgendaCard(
    block: BlockUiModel.Agenda,
    onActionClick: (String) -> Unit
) {
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    CardContainer {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val halfWidthPx = with(density) { (maxWidth / 2).roundToPx() }
            val itemWidthPx = with(density) { 50.dp.roundToPx() }
            val centerOffsetPx = -(halfWidthPx - itemWidthPx / 2)

            fun centerDay(index: Int) {
                if (index in block.days.indices) {
                    coroutineScope.launch {
                        lazyListState.animateScrollToItem(index, centerOffsetPx)
                    }
                }
            }

            // Auto-centralização suave ao mudar de dia ou carregar
            val selectedKey = remember(block.days) {
                block.days.firstOrNull { it.isSelected }?.fullDateStr
                    ?: block.days.firstOrNull { it.isToday }?.fullDateStr
            }
            LaunchedEffect(selectedKey, block.days.size) {
                val targetIndex = block.days.indexOfFirst { it.isSelected }.let {
                    if (it >= 0) it else block.days.indexOfFirst { d -> d.isToday }
                }
                if (targetIndex >= 0) {
                    lazyListState.animateScrollToItem(targetIndex, centerOffsetPx)
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                // ── Header ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = block.title, style = EngineTypography.titleMedium)
                        if (!block.subtitle.isNullOrEmpty()) {
                            Text(
                                text = block.subtitle,
                                style = EngineTypography.labelSmall,
                                color = EngineTheme.colors.textSecondary
                            )
                        }
                    }

                    // Opção Hoje no topo direito (reseta o scroll e centraliza o dia de hoje)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(EngineTheme.colors.cardBorder.copy(alpha = 0.5f))
                            .clickable {
                                val todayIdx = block.days.indexOfFirst { it.isToday }
                                if (todayIdx >= 0) {
                                    centerDay(todayIdx)
                                }
                                onActionClick("a today")
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "Hoje",
                            style = EngineTypography.labelSmall,
                            color = EngineTheme.colors.accentCyan
                        )
                    }
                }

                if (block.isOpen) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 480.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Secao Calendario (Carrossel Horizontal de Dias da Semana)
                        if (block.days.isNotEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (block.monthLabel.isNotBlank()) {
                                    Text(
                                        text = block.monthLabel,
                                        style = EngineTypography.labelMedium,
                                        color = EngineTheme.colors.accentCyan,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                }

                                LazyRow(
                                    state = lazyListState,
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(horizontal = 2.dp)
                                ) {
                                    itemsIndexed(block.days) { index, day ->
                                        val isToday = day.isToday
                                        val isSelected = day.isSelected
                                        val bg = when {
                                            isSelected -> EngineTheme.colors.accentCyan.copy(alpha = 0.18f)
                                            isToday -> EngineTheme.colors.cardBorder.copy(alpha = 0.6f)
                                            else -> EngineTheme.colors.background
                                        }
                                        val borderCol = when {
                                            isSelected -> EngineTheme.colors.accentCyan
                                            isToday -> EngineTheme.colors.textMuted
                                            else -> EngineTheme.colors.cardBorder
                                        }

                                        Column(
                                            modifier = Modifier
                                                .width(50.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(bg)
                                                .border(1.dp, borderCol, RoundedCornerShape(8.dp))
                                                .clickable {
                                                    centerDay(index)
                                                    if (day.commandToExecute != null) onActionClick(day.commandToExecute)
                                                }
                                                .padding(vertical = 8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = day.dayOfWeekLabel.ifEmpty { "D" },
                                                style = EngineTypography.labelSmall,
                                                color = if (isSelected) EngineTheme.colors.accentCyan else EngineTheme.colors.textSecondary
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = if (day.dayNumber > 0) String.format("%02d", day.dayNumber) else "--",
                                                style = EngineTypography.titleMedium,
                                                color = if (isSelected) EngineTheme.colors.accentCyan else if (isToday) EngineTheme.colors.accentCyan else EngineTheme.colors.textPrimary
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                modifier = Modifier.height(6.dp),
                                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (day.hasEvents) {
                                                    Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(EngineTheme.colors.accentAmber))
                                                }
                                                if (day.hasTasks) {
                                                    Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(EngineTheme.colors.accentGreen))
                                                }
                                                if (!day.hasEvents && !day.hasTasks) {
                                                    Spacer(modifier = Modifier.size(5.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Divisoria
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(EngineTheme.colors.cardBorder))
                        }

                // Secao de Eventos
                if (block.events.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        block.events.forEach { event ->
                            val parsedColor = if (!event.colorHex.isNullOrBlank()) {
                                try {
                                    val clean = if (event.colorHex.startsWith("#")) event.colorHex else "#${event.colorHex}"
                                    Color(android.graphics.Color.parseColor(clean))
                                } catch (_: Exception) {
                                    EngineTheme.colors.accentCyan
                                }
                            } else {
                                EngineTheme.colors.accentCyan
                            }
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .then(
                                        if (event.commandToExecute != null) {
                                            Modifier.clickable { onActionClick(event.commandToExecute) }
                                        } else Modifier
                                    )
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Barra lateral colorida
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(24.dp)
                                        .background(parsedColor, RoundedCornerShape(1.dp))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = event.title,
                                        style = EngineTypography.bodyMedium,
                                        color = EngineTheme.colors.textPrimary
                                    )
                                    if (event.location != null && event.location.isNotBlank()) {
                                        Text(
                                            text = event.location,
                                            style = EngineTypography.labelSmall,
                                            color = EngineTheme.colors.textMuted
                                        )
                                    }
                                }
                                
                                Text(
                                    text = event.timeLabel,
                                    style = EngineTypography.labelSmall,
                                    color = EngineTheme.colors.textSecondary,
                                    textAlign = TextAlign.End
                                )

                                if (event.deleteCommand != null) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable { onActionClick(event.deleteCommand) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "✕",
                                            style = EngineTypography.labelMedium,
                                            color = EngineTheme.colors.textMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Linha divisória se tiver ambos
                if (block.events.isNotEmpty() && block.tasks.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(EngineTheme.colors.cardBorder)
                    )
                }

                // Secao de Tarefas
                if (block.tasks.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        block.tasks.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .then(
                                        if (item.isChecklist && item.toggleCommand != null) {
                                            Modifier.clickable { onActionClick(item.toggleCommand) }
                                        } else Modifier
                                    )
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (item.isChecklist) {
                                    val boxColor = if (item.isChecked) EngineTheme.colors.accentGreen
                                        else EngineTheme.colors.cardBorder
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .border(1.5f.dp, boxColor, RoundedCornerShape(3.dp))
                                            .background(
                                                if (item.isChecked) EngineTheme.colors.accentGreen.copy(alpha = 0.15f)
                                                else Color.Transparent
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (item.isChecked) {
                                            Text(
                                                text = "✓",
                                                style = EngineTypography.labelSmall,
                                                color = EngineTheme.colors.accentGreen
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                } else {
                                    Text(
                                        text = "•",
                                        style = EngineTypography.bodyMedium,
                                        color = EngineTheme.colors.accentCyan,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                }
        
                                Text(
                                    text = item.text,
                                    style = EngineTypography.bodyMedium,
                                    color = if (item.isChecked) EngineTheme.colors.textMuted
                                        else EngineTheme.colors.textPrimary,
                                    textDecoration = if (item.isChecked) TextDecoration.LineThrough
                                        else TextDecoration.None,
                                    modifier = Modifier.weight(1f)
                                )
        
                                if (item.deleteCommand != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable { onActionClick(item.deleteCommand) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "✕",
                                            style = EngineTypography.labelSmall,
                                            color = EngineTheme.colors.accentRed.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Estado Vazio (quando não há eventos nem tarefas no dia selecionado)
                if (block.events.isEmpty() && block.tasks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(EngineTheme.colors.cardBorder.copy(alpha = 0.25f))
                            .padding(vertical = 18.dp, horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nenhum evento ou tarefa neste dia.",
                            style = EngineTypography.bodyMedium,
                            color = EngineTheme.colors.textMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // ── Actions ──
        if (block.actions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                block.actions.forEach { action ->
                    Text(
                        text = action.label,
                        style = EngineTypography.titleMedium,
                        color = EngineTheme.colors.accentCyan,
                        modifier = Modifier
                            .clickable { onActionClick(action.commandToExecute) }
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }

        // ── Input Field ──
        if (block.inputHint != null && block.inputCommand != null) {
            Spacer(modifier = Modifier.height(12.dp))
            var textState by remember { mutableStateOf(TextFieldValue("")) }
            val focusManager = LocalFocusManager.current
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(EngineTheme.colors.background, RoundedCornerShape(4.dp))
                    .border(1.dp, EngineTheme.colors.cardBorder, RoundedCornerShape(4.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = textState,
                    onValueChange = { textState = it },
                    textStyle = EngineTypography.bodyMedium.copy(color = EngineTheme.colors.textPrimary),
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (textState.text.isNotBlank()) {
                                onActionClick("${block.inputCommand} ${textState.text}")
                                textState = TextFieldValue("")
                                focusManager.clearFocus()
                            }
                        }
                    ),
                    decorationBox = { innerTextField ->
                        if (textState.text.isEmpty()) {
                            Text(
                                text = block.inputHint,
                                style = EngineTypography.bodyMedium,
                                color = EngineTheme.colors.textMuted
                            )
                        }
                        innerTextField()
                    }
                )
                if (textState.text.isNotBlank()) {
                    Text(
                        text = "Enviar",
                        style = EngineTypography.labelMedium,
                        color = EngineTheme.colors.accentGreen,
                        modifier = Modifier
                            .clickable {
                                onActionClick("${block.inputCommand} ${textState.text}")
                                textState = TextFieldValue("")
                                focusManager.clearFocus()
                            }
                            .padding(start = 8.dp)
                    )
                }
            }
        }
    }
}
}
}
