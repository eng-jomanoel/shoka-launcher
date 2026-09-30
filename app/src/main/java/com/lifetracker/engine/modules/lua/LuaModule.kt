package com.lifetracker.engine.modules.lua

import androidx.compose.ui.graphics.Color
import com.lifetracker.engine.core.model.BlockUiModel
import com.lifetracker.engine.core.module.CommandResult
import com.lifetracker.engine.core.module.EngineModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.JsePlatform
import java.io.File


class LuaModule(
    val scriptFile: File,
    private val bridge: LuaEngineBridge
) : EngineModule {

    private val globals: Globals = JsePlatform.standardGlobals()
    private var moduleTable: LuaTable? = null

    override var id: String = scriptFile.nameWithoutExtension
    override var name: String = scriptFile.nameWithoutExtension
    override var commandPrefix: String = scriptFile.nameWithoutExtension.take(1)
    override var helpText: String = "$commandPrefix - Módulo Lua"

    private val _blockFlow = MutableStateFlow<BlockUiModel?>(null)
    override val blockFlow: StateFlow<BlockUiModel?> = _blockFlow.asStateFlow()
    override val hasUi: Boolean
        get() = true

    init {
        reload()
    }

    fun reload(): Boolean {
        return try {
            bridge.inject(globals)
            val chunk = globals.loadfile(scriptFile.absolutePath)
            val result = chunk.call()
            
            if (result.istable()) {
                moduleTable = result.checktable()
                
                val modId = moduleTable?.get("id")
                if (modId != null && !modId.isnil()) id = modId.tojstring()

                val modName = moduleTable?.get("name")
                if (modName != null && !modName.isnil()) name = modName.tojstring()

                val modPrefix = moduleTable?.get("prefix")
                if (modPrefix != null && !modPrefix.isnil()) commandPrefix = modPrefix.tojstring()

                val modHelp = moduleTable?.get("help")
                if (modHelp != null && !modHelp.isnil()) helpText = modHelp.tojstring()

                updateUi()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _blockFlow.value = BlockUiModel.Info(
                moduleId = id,
                title = "Erro Lua: $name",
                description = e.message ?: "Erro ao carregar script",
                tag = "ERR",
                tagColor = Color.Red
            )
            false
        }
    }

    private var tickerJob: Job? = null

    fun updateUi() {
        try {
            val renderFunc = moduleTable?.get("render")
            if (renderFunc != null && renderFunc.isfunction()) {
                val blockVal = renderFunc.call()
                if (blockVal.istable()) {
                    val table = blockVal.checktable()
                    val block = parseBlock(table)
                    _blockFlow.value = block
                    checkTicker(block)
                    return
                }
            }
            _blockFlow.value = null
            stopTicker()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkTicker(block: BlockUiModel?) {
        val needsTicker = (block is BlockUiModel.Media && block.isPlaying) ||
                          (block is BlockUiModel.Workout && block.isTimerActive)
        if (needsTicker) {
            if (tickerJob == null || tickerJob?.isActive != true) {
                tickerJob = CoroutineScope(Dispatchers.Main).launch {
                    while (isActive) {
                        delay(1000)
                        updateUi()
                    }
                }
            }
        } else {
            stopTicker()
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }


    private fun parseBlock(table: LuaTable): BlockUiModel? {
        val blockTitle = table.get("title").optjstring(name)
        val type = table.get("type").optjstring("")

        // Verifica se é tipo Workout (Academia com timer de descanso e controle de séries)
        if (type == "workout") {
            val actions = mutableListOf<BlockUiModel.BlockAction>()
            val actionsVal = table.get("actions")
            if (!actionsVal.isnil() && actionsVal.istable()) {
                val actionTable = actionsVal.checktable()
                for (i in 1..actionTable.length()) {
                    val act = actionTable.get(i)
                    if (act.istable()) {
                        actions.add(BlockUiModel.BlockAction(
                            label = act.get("label").optjstring("[Botão]"),
                            commandToExecute = act.get("cmd").optjstring("")
                        ))
                    }
                }
            }

            val exercises = mutableListOf<BlockUiModel.WorkoutExerciseItem>()
            val exercisesVal = table.get("exercises")
            if (!exercisesVal.isnil() && exercisesVal.istable()) {
                val exTable = exercisesVal.checktable()
                for (i in 1..exTable.length()) {
                    val ex = exTable.get(i)
                    if (ex.istable()) {
                        exercises.add(BlockUiModel.WorkoutExerciseItem(
                            index = ex.get("index").optint(i),
                            name = ex.get("name").optjstring("Exercício"),
                            info = ex.get("info").optjstring(""),
                            isCompleted = ex.get("completed").optboolean(false),
                            isCurrent = ex.get("current").optboolean(false),
                            commandToExecute = ex.get("cmd").optjstring("")
                        ))
                    }
                }
            }

            val days = mutableListOf<BlockUiModel.WorkoutDayItem>()
            val daysVal = table.get("days")
            if (!daysVal.isnil() && daysVal.istable()) {
                val dayTable = daysVal.checktable()
                for (i in 1..dayTable.length()) {
                    val d = dayTable.get(i)
                    if (d.istable()) {
                        days.add(BlockUiModel.WorkoutDayItem(
                            dayId = d.get("day_id").optjstring(""),
                            label = d.get("label").optjstring("Dia"),
                            isCurrent = d.get("is_current").optboolean(false),
                            isToday = d.get("is_today").optboolean(false),
                            commandToExecute = d.get("cmd").optjstring("")
                        ))
                    }
                }
            }

            val decWeightCmd = table.get("dec_weight_cmd").optjstring("g w-")
            val incWeightCmd = table.get("inc_weight_cmd").optjstring("g w+")
            val decRepsCmd = table.get("dec_reps_cmd").optjstring("g r-")
            val incRepsCmd = table.get("inc_reps_cmd").optjstring("g r+")

            val weightVal = table.get("weight")
            val weightFloat = if (!weightVal.isnil()) weightVal.tofloat() else 0f

            return BlockUiModel.Workout(
                moduleId = id,
                title = blockTitle,
                dayName = table.get("day_name").optjstring("Treino"),
                exerciseName = table.get("exercise_name").optjstring("Exercício Atual"),
                currentSet = table.get("current_set").optint(1),
                totalSets = table.get("total_sets").optint(1),
                targetReps = table.get("target_reps").optjstring("8-12"),
                weight = weightFloat,
                repsLogged = table.get("reps_logged").optint(10),
                isTimerActive = table.get("is_timer_active").optboolean(false),
                timerRemainingSeconds = table.get("timer_remaining").optint(0),
                timerTotalSeconds = table.get("timer_total").optint(60),
                progressText = table.get("progress_text").optjstring(null),
                actions = actions,
                exercises = exercises,
                showExerciseList = table.get("show_list").optboolean(false),
                days = days,
                decWeightCmd = decWeightCmd,
                incWeightCmd = incWeightCmd,
                decRepsCmd = decRepsCmd,
                incRepsCmd = incRepsCmd
            )
        }


        // Verifica se é tipo Media (Player de música rico)
        if (type == "media" || !table.get("cover").isnil() || !table.get("items").isnil()) {
            val actions = mutableListOf<BlockUiModel.BlockAction>()
            val actionsVal = table.get("actions")
            if (!actionsVal.isnil() && actionsVal.istable()) {
                val actionTable = actionsVal.checktable()
                for (i in 1..actionTable.length()) {
                    val act = actionTable.get(i)
                    if (act.istable()) {
                        actions.add(BlockUiModel.BlockAction(
                            label = act.get("label").optjstring("[Botão]"),
                            commandToExecute = act.get("cmd").optjstring("")
                        ))
                    }
                }
            }

            val items = mutableListOf<BlockUiModel.MediaItem>()
            val itemsVal = table.get("items")
            if (!itemsVal.isnil() && itemsVal.istable()) {
                val itemTable = itemsVal.checktable()
                for (i in 1..itemTable.length()) {
                    val item = itemTable.get(i)
                    if (item.istable()) {
                        items.add(BlockUiModel.MediaItem(
                            label = item.get("label").optjstring(""),
                            sublabel = item.get("sublabel").optjstring(null),
                            commandToExecute = item.get("cmd").optjstring(""),
                            isActive = item.get("active").optboolean(false)
                        ))
                    }
                }
            }

            val progressVal = table.get("progress")
            val progressFloat = if (!progressVal.isnil()) progressVal.tofloat() else null

            val shuffleVal = table.get("shuffle")
            val shuffleBool = if (!shuffleVal.isnil()) shuffleVal.optboolean(false) else false

            val loopVal = table.get("loop")
            val loopStr = if (!loopVal.isnil()) loopVal.optjstring("off") else "off"

            return BlockUiModel.Media(
                moduleId = id,
                title = blockTitle,
                subtitle = table.get("subtitle").optjstring(null),
                coverPath = table.get("cover").optjstring(null),
                isPlaying = table.get("is_playing").optboolean(false),
                progress = progressFloat,
                progressText = table.get("progress_text").optjstring(null),
                shuffle = shuffleBool,
                loopMode = loopStr,
                actions = actions,
                items = items
            )

        }

        // Verifica se é tipo Progress
        val currentVal = table.get("current")
        val targetVal = table.get("target")
        if (!currentVal.isnil() && !targetVal.isnil()) {
            return BlockUiModel.Progress(
                moduleId = id,
                title = blockTitle,
                current = currentVal.tofloat(),
                target = targetVal.tofloat(),
                unit = table.get("unit").optjstring(""),
                barColor = Color.Green
            )
        }

        // Verifica se é tipo Interactive com botões
        val actionsVal = table.get("actions")
        if (!actionsVal.isnil() && actionsVal.istable()) {
            val actionTable = actionsVal.checktable()
            val actions = mutableListOf<BlockUiModel.BlockAction>()
            for (i in 1..actionTable.length()) {
                val act = actionTable.get(i)
                if (act.istable()) {
                    val label = act.get("label").optjstring("[Botão]")
                    val cmd = act.get("cmd").optjstring("")
                    actions.add(BlockUiModel.BlockAction(label, cmd))
                }
            }
            val status = table.get("status").optjstring("")
            return BlockUiModel.Interactive(
                moduleId = id,
                title = blockTitle,
                status = status,
                actions = actions
            )
        }

        // Fallback: Bloco Info
        val desc = table.get("description").optjstring(table.get("status").optjstring(""))
        val tag = table.get("tag").optjstring(null)
        return BlockUiModel.Info(
            moduleId = id,
            title = blockTitle,
            description = desc,
            tag = tag,
            tagColor = Color.Cyan
        )
    }

    override suspend fun executeCommand(args: String): CommandResult {
        return try {
            val trimmed = args.trim()
            if (trimmed == "-h" || trimmed == "--help") {
                return CommandResult.Success("MÓDULO LUA: $name ($commandPrefix)\nAjuda: $helpText")
            }

            val onCommandFunc = moduleTable?.get("on_command")
            if (onCommandFunc != null && onCommandFunc.isfunction()) {
                val res = onCommandFunc.call(LuaValue.valueOf(trimmed))
                updateUi() // Atualiza a UI após executar o comando
                if (res.isstring()) {
                    CommandResult.Success(res.tojstring())
                } else {
                    CommandResult.Success("Comando executado.")
                }
            } else {
                CommandResult.Error("O script não implementou a função 'on_command(args)'.")
            }
        } catch (e: LuaError) {
            CommandResult.Error("Erro Lua: ${e.message}")
        } catch (e: Exception) {
            CommandResult.Error("Erro: ${e.message}")
        }
    }
}
