package com.lifetracker.engine.core.module

import android.content.Context
import android.os.Environment
import androidx.compose.ui.graphics.Color
import com.lifetracker.engine.core.model.BlockUiModel
import com.lifetracker.engine.ui.theme.AmoledColorScheme
import com.lifetracker.engine.ui.theme.EngineColors
import com.lifetracker.engine.ui.theme.SolarizedDarkColorScheme
import com.lifetracker.engine.ui.theme.SolarizedLightColorScheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaTable
import org.luaj.vm2.lib.jse.JsePlatform
import java.io.File

/**
 * Módulo nativo do Launcher responsável por gerenciar a aparência visual.
 * Carrega temas definidos dinamicamente em scripts Lua na pasta Documents/Shoka/themes/.
 * Prefixo: "t" (theme)
 * Exemplo: "t list", "t dracula", "t amoled", "t solarized_dark"
 */
class ThemeModule(private val context: Context) : EngineModule {

    override val id: String = "theme_module"
    override val name: String = "Temas"
    override val commandPrefix: String = "t"
    override val helpText: String = "t <nome> | t list | t reload | t toggle"

    private val _blockFlow = MutableStateFlow<BlockUiModel?>(null)
    override val blockFlow: StateFlow<BlockUiModel?> = _blockFlow.asStateFlow()

    override fun getSuggestions(args: String): List<CommandSuggestion> {
        val query = args.trim().lowercase()
        val themeFiles = themesDir.listFiles { f -> f.extension == "lua" }?.map { it.nameWithoutExtension } ?: emptyList()
        val builtIns = listOf("amoled", "solarized_dark", "solarized_light")
        val allThemes = (builtIns + themeFiles).distinct()
        
        val base = listOf(
            CommandSuggestion("t toggle", "t toggle", true),
            CommandSuggestion("t list", "t list", true),
            CommandSuggestion("t reload", "t reload", true)
        )
        val themeSuggestions = allThemes.map { name ->
            CommandSuggestion("t $name", "t $name", true)
        }
        val combined = base + themeSuggestions
        return if (query.isEmpty()) combined else combined.filter { it.command.contains(query) }
    }

    private val prefs by lazy {
        context.getSharedPreferences("modular_theme_prefs", Context.MODE_PRIVATE)
    }

    val themesDir: File by lazy {
        val docs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val dir = File(docs, "Shoka/themes").apply { mkdirs() }
        val oldDir = File(docs, "ModularLife/themes")
        if (oldDir.exists() && oldDir.isDirectory) {
            try {
                oldDir.copyRecursively(dir, overwrite = false)
            } catch (_: Exception) {}
        }
        dir
    }

    private val _currentThemeName = MutableStateFlow("amoled")
    val currentThemeName: StateFlow<String> = _currentThemeName.asStateFlow()

    private val _currentTheme = MutableStateFlow(AmoledColorScheme)
    val currentTheme: StateFlow<EngineColors> = _currentTheme.asStateFlow()

    init {
        ensureDefaultThemes()
        val saved = prefs.getString("saved_theme", "amoled") ?: "amoled"
        applyTheme(saved, save = false)
    }

    fun ensureDefaultThemes() {
        val amoledFile = File(themesDir, "amoled.lua")
        if (!amoledFile.exists()) {
            amoledFile.writeText(
                """
return {
    name = "AMOLED Black",
    background = "#000000",
    card = "#0a0a0a",
    card_border = "#1a1a1a",
    text_primary = "#ffffff",
    text_secondary = "#888888",
    text_muted = "#444444",
    accent_green = "#00ff66",
    accent_cyan = "#00ddff",
    accent_amber = "#ffb300",
    accent_red = "#ff3333"
}
                """.trimIndent()
            )
        }

        val solarizedDarkFile = File(themesDir, "solarized_dark.lua")
        if (!solarizedDarkFile.exists()) {
            solarizedDarkFile.writeText(
                """
return {
    name = "Solarized Dark",
    background = "#002b36",
    card = "#073642",
    card_border = "#586e75",
    text_primary = "#eee8d5",
    text_secondary = "#93a1a1",
    text_muted = "#839496",
    accent_green = "#859900",
    accent_cyan = "#268bd2",
    accent_amber = "#b58900",
    accent_red = "#dc322f"
}
                """.trimIndent()
            )
        }

        val solarizedLightFile = File(themesDir, "solarized_light.lua")
        if (!solarizedLightFile.exists()) {
            solarizedLightFile.writeText(
                """
return {
    name = "Solarized Light",
    background = "#fdf6e3",
    card = "#eee8d5",
    card_border = "#93a1a1",
    text_primary = "#002b36",
    text_secondary = "#586e75",
    text_muted = "#657b83",
    accent_green = "#859900",
    accent_cyan = "#268bd2",
    accent_amber = "#b58900",
    accent_red = "#dc322f"
}
                """.trimIndent()
            )
        }

        val draculaFile = File(themesDir, "dracula.lua")
        if (!draculaFile.exists()) {
            draculaFile.writeText(
                """
return {
    name = "Dracula",
    background = "#282a36",
    card = "#44475a",
    card_border = "#6272a4",
    text_primary = "#f8f8f2",
    text_secondary = "#8be9fd",
    text_muted = "#6272a4",
    accent_green = "#50fa7b",
    accent_cyan = "#8be9fd",
    accent_amber = "#f1fa8c",
    accent_red = "#ff5555"
}
                """.trimIndent()
            )
        }

        val nordFile = File(themesDir, "nord.lua")
        if (!nordFile.exists()) {
            nordFile.writeText(
                """
return {
    name = "Nord Frost",
    background = "#2e3440",
    card = "#3b4252",
    card_border = "#4c566a",
    text_primary = "#eceff4",
    text_secondary = "#d8dee9",
    text_muted = "#e5e9f0",
    accent_green = "#a3be8c",
    accent_cyan = "#88c0d0",
    accent_amber = "#ebcb8b",
    accent_red = "#bf616a"
}
                """.trimIndent()
            )
        }
    }

    private fun parseHex(hex: String?, fallback: Color): Color {
        if (hex.isNullOrBlank()) return fallback
        return try {
            val clean = if (hex.startsWith("#")) hex else "#$hex"
            Color(android.graphics.Color.parseColor(clean))
        } catch (_: Exception) {
            fallback
        }
    }

    private fun loadThemeFromFile(file: File): EngineColors? {
        return try {
            val globals: Globals = JsePlatform.standardGlobals()
            val chunk = globals.loadfile(file.absolutePath)
            val result = chunk.call()
            if (result.istable()) {
                val t = result.checktable()
                EngineColors(
                    background = parseHex(t.get("background").optjstring("#000000"), Color.Black),
                    card = parseHex(t.get("card").optjstring("#0a0a0a"), Color.DarkGray),
                    cardBorder = parseHex(t.get("card_border").optjstring("#1a1a1a"), Color.Gray),
                    textPrimary = parseHex(t.get("text_primary").optjstring("#ffffff"), Color.White),
                    textSecondary = parseHex(t.get("text_secondary").optjstring("#888888"), Color.LightGray),
                    textMuted = parseHex(t.get("text_muted").optjstring("#444444"), Color.Gray),
                    accentGreen = parseHex(t.get("accent_green").optjstring("#00ff66"), Color.Green),
                    accentCyan = parseHex(t.get("accent_cyan").optjstring("#00ddff"), Color.Cyan),
                    accentAmber = parseHex(t.get("accent_amber").optjstring("#ffb300"), Color.Yellow),
                    accentRed = parseHex(t.get("accent_red").optjstring("#ff3333"), Color.Red)
                )
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun applyTheme(themeName: String, save: Boolean = true): Boolean {
        val targetName = themeName.lowercase().removeSuffix(".lua")
        val file = File(themesDir, "$targetName.lua")

        val loaded = if (file.exists()) {
            loadThemeFromFile(file)
        } else {
            // Fallback para os embutidos se o arquivo não existir
            when (targetName) {
                "amoled" -> AmoledColorScheme
                "solarized_dark", "dark" -> SolarizedDarkColorScheme
                "solarized_light", "light" -> SolarizedLightColorScheme
                else -> null
            }
        }

        return if (loaded != null) {
            _currentTheme.value = loaded
            _currentThemeName.value = targetName
            if (save) {
                prefs.edit().putString("saved_theme", targetName).apply()
            }
            true
        } else {
            false
        }
    }

    override suspend fun executeCommand(args: String): CommandResult {
        val query = args.trim().lowercase()

        if (query == "-h" || query == "--help") {
            return CommandResult.Success(
                "MÓDULO DE TEMA (t)\n" +
                "  t list       : Lista todos os temas disponíveis na pasta\n" +
                "  t <nome>     : Aplica e salva o tema (ex: t dracula, t nord)\n" +
                "  t reload     : Recarrega o tema atual do arquivo\n" +
                "  t toggle     : Alterna para o próximo tema da lista\n" +
                "  t -h         : Mostra esta ajuda"
            )
        }

        if (query == "list") {
            ensureDefaultThemes()
            val files = themesDir.listFiles { f -> f.extension == "lua" } ?: emptyArray()
            val sb = StringBuilder("TEMAS DISPONÍVEIS (Documents/Shoka/themes):\n")
            val current = _currentThemeName.value
            files.forEach { f ->
                val name = f.nameWithoutExtension
                if (name == current) {
                    sb.append("  * $name [ATIVO]\n")
                } else {
                    sb.append("    $name\n")
                }
            }
            sb.append("\nDigite 't <nome>' para aplicar e salvar.")
            return CommandResult.Success(sb.toString())
        }

        if (query == "reload") {
            val current = _currentThemeName.value
            return if (applyTheme(current, save = false)) {
                CommandResult.Success("Tema '$current' recarregado do arquivo!")
            } else {
                CommandResult.Error("Falha ao recarregar tema '$current'.")
            }
        }

        if (query.isEmpty() || query == "toggle") {
            ensureDefaultThemes()
            val files = themesDir.listFiles { f -> f.extension == "lua" }?.map { it.nameWithoutExtension } ?: listOf("amoled", "solarized_dark", "solarized_light")
            val currentIdx = files.indexOf(_currentThemeName.value)
            val nextName = if (currentIdx != -1 && currentIdx < files.size - 1) files[currentIdx + 1] else files.firstOrNull() ?: "amoled"
            applyTheme(nextName, save = true)
            return CommandResult.Success("Tema alterado para: $nextName")
        }

        return if (applyTheme(query, save = true)) {
            CommandResult.Success("Tema '$query' aplicado e salvo com sucesso!")
        } else {
            CommandResult.Error("Tema '$query' não encontrado em Documents/Shoka/themes. Digite 't list'.")
        }
    }
}
