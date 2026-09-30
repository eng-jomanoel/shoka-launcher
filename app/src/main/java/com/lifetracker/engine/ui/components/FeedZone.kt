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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
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
            modifier = Modifier.fillMaxWidth()
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
                modifier = Modifier.fillMaxWidth()
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
                            .clickable { onActionClick(day.commandToExecute) }
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

        // Load / Weight & Reps Stepper Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Weight Stepper (Left Box)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .background(EngineTheme.colors.background, RoundedCornerShape(6.dp))
                    .border(1.dp, EngineTheme.colors.cardBorder, RoundedCornerShape(6.dp))
                    .padding(vertical = 4.dp, horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Minus Weight Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(EngineTheme.colors.card)
                        .border(1.dp, EngineTheme.colors.cardBorder, RoundedCornerShape(4.dp))
                        .clickable { onActionClick(block.decWeightCmd) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "−",
                        style = EngineTypography.titleMedium,
                        color = EngineTheme.colors.accentAmber
                    )
                }

                // Weight Label
                val weightStr = if (block.weight % 1f == 0f) "${block.weight.toInt()} KG" else "${block.weight} KG"
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "CARGA",
                        style = EngineTypography.labelSmall,
                        color = EngineTheme.colors.textMuted
                    )
                    Text(
                        text = weightStr,
                        style = EngineTypography.titleMedium,
                        color = EngineTheme.colors.textPrimary
                    )
                }

                // Plus Weight Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(EngineTheme.colors.card)
                        .border(1.dp, EngineTheme.colors.cardBorder, RoundedCornerShape(4.dp))
                        .clickable { onActionClick(block.incWeightCmd) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        style = EngineTypography.titleMedium,
                        color = EngineTheme.colors.accentAmber
                    )
                }
            }

            // Reps Stepper (Right Box)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .background(EngineTheme.colors.background, RoundedCornerShape(6.dp))
                    .border(1.dp, EngineTheme.colors.cardBorder, RoundedCornerShape(6.dp))
                    .padding(vertical = 4.dp, horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Minus Reps Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(EngineTheme.colors.card)
                        .border(1.dp, EngineTheme.colors.cardBorder, RoundedCornerShape(4.dp))
                        .clickable { onActionClick(block.decRepsCmd) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "−",
                        style = EngineTypography.titleMedium,
                        color = EngineTheme.colors.accentGreen
                    )
                }

                // Reps Label
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "REPS FEITAS",
                        style = EngineTypography.labelSmall,
                        color = EngineTheme.colors.textMuted
                    )
                    Text(
                        text = "${block.repsLogged}",
                        style = EngineTypography.titleMedium,
                        color = EngineTheme.colors.accentGreen
                    )
                }

                // Plus Reps Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(EngineTheme.colors.card)
                        .border(1.dp, EngineTheme.colors.cardBorder, RoundedCornerShape(4.dp))
                        .clickable { onActionClick(block.incRepsCmd) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        style = EngineTypography.titleMedium,
                        color = EngineTheme.colors.accentGreen
                    )
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
                            .clickable { onActionClick(action.commandToExecute) }
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
                            .clickable { onActionClick(ex.commandToExecute) }
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
    }
}
