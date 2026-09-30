package com.lifetracker.engine.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lifetracker.engine.ui.theme.EngineTheme
import com.lifetracker.engine.ui.theme.EngineTypography
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Zona 1: Topo (Contexto)
 * Otimizado para baixíssimo consumo de CPU no Helio P35:
 * - Event-driven battery receiver (zero IPC polling)
 * - Clock atualiza a cada 10s (em vez de 1s com alocação contínua)
 */
@Composable
fun HeaderZone(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault()) }

    var currentTime by remember { mutableStateOf(timeFormat.format(Date())) }
    var currentDate by remember { mutableStateOf(dateFormat.format(Date())) }
    var batteryLevel by remember { mutableIntStateOf(100) }
    var ramUsageInfo by remember { mutableStateOf("RAM: --") }

    // Registra listener de bateria orientado a evento (sem polling contínuo)
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                intent?.let {
                    val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    if (level >= 0 && scale > 0) {
                        batteryLevel = (level * 100 / scale)
                    }
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val stickyIntent = context.registerReceiver(receiver, filter)
        // Pega o valor inicial do sticky broadcast
        stickyIntent?.let {
            val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) {
                batteryLevel = (level * 100 / scale)
            }
        }

        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }
    }

    // Atualiza o relógio e a RAM a cada 5 segundos
    LaunchedEffect(Unit) {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memoryInfo = android.app.ActivityManager.MemoryInfo()
        
        while (true) {
            val now = Date()
            val newTime = timeFormat.format(now)
            if (newTime != currentTime) {
                currentTime = newTime
                currentDate = dateFormat.format(now)
            }
            
            // Otimização: Leitura de RAM é leve se feita em intervalos razoáveis
            activityManager.getMemoryInfo(memoryInfo)
            val totalMb = memoryInfo.totalMem / (1024 * 1024)
            val availMb = memoryInfo.availMem / (1024 * 1024)
            val usedMb = totalMb - availMb
            val percent = if (totalMb > 0) (usedMb * 100) / totalMb else 0
            
            // Formatando em Gigabytes para ficar mais enxuto: "RAM: 1.4/3.8GB (36%)"
            val usedGb = usedMb / 1024f
            val totalGb = totalMb / 1024f
            ramUsageInfo = String.format(Locale.US, "RAM: %.1f/%.1fGB (%d%%)", usedGb, totalGb, percent)

            delay(5_000L) // Reduzido de 10s para 5s para dar mais dinamismo à RAM
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Linha superior: RAM (Esquerda) e Bateria (Direita)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = ramUsageInfo,
                style = EngineTypography.labelSmall,
                color = EngineTheme.colors.textMuted
            )
            
            Text(
                text = androidx.compose.ui.res.stringResource(id = com.lifetracker.engine.R.string.bat_prefix, batteryLevel),
                style = EngineTypography.labelSmall,
                color = EngineTheme.colors.accentGreen
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Relógio grande minimalista
        Text(
            text = currentTime,
            style = EngineTypography.displayLarge,
            color = EngineTheme.colors.textPrimary
        )

        // Data resumida
        Text(
            text = currentDate.uppercase(),
            style = EngineTypography.bodyMedium,
            color = EngineTheme.colors.textMuted
        )
    }
}
