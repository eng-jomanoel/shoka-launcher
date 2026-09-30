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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
                    Text(
                        text = "[Salvar e Concluir]",
                        style = EngineTypography.labelSmall,
                        color = EngineTheme.colors.textPrimary,
                        modifier = Modifier.clickable { onExitEditMode() }
                    )
                }
            }
        }

        items(blocks, key = { it.moduleId }) { block ->
            Column {
                if (isEditMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp, start = 4.dp, end = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Módulo: ${block.moduleId}",
                            style = EngineTypography.labelSmall,
                            color = EngineTheme.colors.textMuted
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "[▲ Sobe]",
                                style = EngineTypography.labelSmall,
                                color = EngineTheme.colors.accentGreen,
                                modifier = Modifier.clickable { onMoveUp(block.moduleId) }
                            )
                            Text(
                                text = "[▼ Desce]",
                                style = EngineTypography.labelSmall,
                                color = EngineTheme.colors.accentGreen,
                                modifier = Modifier.clickable { onMoveDown(block.moduleId) }
                            )
                            Text(
                                text = "[✕ Ocultar]",
                                style = EngineTypography.labelSmall,
                                color = EngineTheme.colors.accentRed,
                                modifier = Modifier.clickable { onHide(block.moduleId) }
                            )
                        }
                    }
                }

                when (block) {
                    is BlockUiModel.Info -> InfoCard(block)
                    is BlockUiModel.Progress -> ProgressCard(block)
                    is BlockUiModel.Interactive -> InteractiveCard(block, onActionClick)
                    is BlockUiModel.Media -> MediaCard(block, onActionClick)
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
