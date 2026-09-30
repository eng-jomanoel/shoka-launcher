package com.lifetracker.engine.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.lifetracker.engine.core.module.CommandResult
import com.lifetracker.engine.ui.theme.EngineTheme
import com.lifetracker.engine.ui.theme.EngineTypography

/**
 * Zona 3: Rodapé (Terminal CLI)
 * Linha de comando para entrada de dados e status do último comando executado.
 */
@Composable
fun CliZone(
    lastResult: CommandResult?,
    currentInput: String,
    onInputChange: (String) -> Unit,
    onExecuteCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val handleSend = {
        if (currentInput.isNotBlank()) {
            onExecuteCommand(currentInput.trim())
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(EngineTheme.colors.background)
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        // Exibição do resultado do último comando (feedback)
        if (lastResult != null) {
            when (lastResult) {
                is CommandResult.Success -> {
                    Text(
                        text = lastResult.message,
                        style = EngineTypography.bodyMedium,
                        color = EngineTheme.colors.accentGreen,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                is CommandResult.Error -> {
                    Text(
                        text = "ERR: ${lastResult.reason}",
                        style = EngineTypography.bodyMedium,
                        color = EngineTheme.colors.accentRed,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                CommandResult.Ignored -> Unit
            }
        }

        // Linha do prompt do terminal (Estilo Linux minimalista)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Text(
                text = "❯ ",
                style = EngineTypography.headlineMedium,
                color = EngineTheme.colors.accentGreen
            )

            BasicTextField(
                value = currentInput,
                onValueChange = onInputChange,
                textStyle = EngineTypography.headlineMedium.copy(color = EngineTheme.colors.textPrimary),
                cursorBrush = SolidColor(EngineTheme.colors.accentGreen),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { handleSend() }),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    if (currentInput.isEmpty()) {
                        Text(
                            text = "_",
                            style = EngineTypography.headlineMedium,
                            color = EngineTheme.colors.textMuted
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}
