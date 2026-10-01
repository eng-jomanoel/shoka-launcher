package com.lifetracker.engine.modules.lua

import androidx.compose.ui.graphics.Color
import com.lifetracker.engine.core.model.BlockUiModel
import com.lifetracker.engine.core.module.CommandResult
import com.lifetracker.engine.core.module.CommandSuggestion
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

    private val luaLock = Any()

    override fun getSuggestions(args: String): List<CommandSuggestion> {
        try {
            val autoFunc = synchronized(luaLock) { moduleTable?.get("autocomplete") }
            if (autoFunc != null && autoFunc.isfunction()) {
                val res = synchronized(luaLock) { autoFunc.call(LuaValue.valueOf(args)) }
                if (res.istable()) {
                    val table = res.checktable()
                    val list = mutableListOf<CommandSuggestion>()
                    for (i in 1..table.length()) {
                        val item = table.get(i)
                        if (item.isstring()) {
                            val subcmd = item.tojstring()
                            val fullCmd = if (subcmd.startsWith("$commandPrefix ")) subcmd else "$commandPrefix $subcmd"
                            list.add(CommandSuggestion(
                                command = fullCmd,
                                displayText = fullCmd,
                                isExecutable = !fullCmd.endsWith(" ")
                            ))
                        } else if (item.istable()) {
                            val itab = item.checktable()
                            val rawCmd = itab.get("command").optjstring(
                                itab.get("cmd").optjstring(itab.get(1).optjstring(""))
                            )
                            val fullCmd = if (rawCmd.startsWith("$commandPrefix ")) rawCmd else "$commandPrefix $rawCmd"
                            val label = itab.get("label").optjstring(fullCmd)
                            val isExec = itab.get("executable").optboolean(!fullCmd.endsWith(" "))
                            list.add(CommandSuggestion(fullCmd, label, isExec))
                        }
                    }
                    if (list.isNotEmpty()) return list
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val q = args.trim().lowercase()
        val block = _blockFlow.value
        val fallbackList = mutableListOf<CommandSuggestion>()
        when (block) {
            is BlockUiModel.Workout -> {
                val cmds = listOf("check", "pular", "files", "load ", "day ", "list", "reset", "uncheck")
                cmds.forEach { c ->
                    fallbackList.add(CommandSuggestion(
                        command = "$commandPrefix $c",
                        displayText = "$commandPrefix $c",
                        isExecutable = !c.endsWith(" ")
                    ))
                }
            }
            is BlockUiModel.Media -> {
                val cmds = listOf("play", "pause", "resume", "next", "prev", "loop", "shuf", "queue", "pl", "scan")
                cmds.forEach { c ->
                    fallbackList.add(CommandSuggestion(
                        command = "$commandPrefix $c",
                        displayText = "$commandPrefix $c",
                        isExecutable = !c.endsWith(" ")
                    ))
                }
            }
            is BlockUiModel.Notes -> {
                val cmds = listOf("add ", "clear", "list")
                cmds.forEach { c ->
                    fallbackList.add(CommandSuggestion(
                        command = "$commandPrefix $c",
                        displayText = "$commandPrefix $c",
                        isExecutable = !c.endsWith(" ")
                    ))
                }
            }
            is BlockUiModel.Agenda -> {
                val cmds = listOf("today", "hoje", "vw month", "vw week", "vw day", "next", "prev", "clear", "clear_evt", "[] ", "todo ", "evt ", "sel ")
                cmds.forEach { c ->
                    fallbackList.add(CommandSuggestion(
                        command = "$commandPrefix $c",
                        displayText = "$commandPrefix $c",
                        isExecutable = !c.endsWith(" ")
                    ))
                }
            }
            else -> {}
        }
        return if (q.isEmpty()) fallbackList else fallbackList.filter { it.command.contains(q) }
    }

    init {
        reload()
    }

    fun reload(): Boolean {
        return synchronized(luaLock) {
            try {
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
    }

    private var tickerJob: Job? = null

    fun updateUi() {
        synchronized(luaLock) {
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

            val files = mutableListOf<BlockUiModel.WorkoutFileItem>()
            val filesVal = table.get("files")
            if (!filesVal.isnil() && filesVal.istable()) {
                val fileTable = filesVal.checktable()
                for (i in 1..fileTable.length()) {
                    val f = fileTable.get(i)
                    if (f.istable()) {
                        files.add(BlockUiModel.WorkoutFileItem(
                            name = f.get("name").optjstring(""),
                            isActive = f.get("is_active").optboolean(false),
                            commandToExecute = f.get("cmd").optjstring("")
                        ))
                    }
                }
            }

            val showFileList = table.get("show_files").optboolean(false)

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
                files = files,
                showFileList = showFileList,
                decWeightCmd = decWeightCmd,
                incWeightCmd = incWeightCmd,
                decRepsCmd = decRepsCmd,
                incRepsCmd = incRepsCmd
            )
        }

        // Verifica se é tipo Diet (Dieta e Nutrição)
        if (type == "diet") {
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

            val currentMealItems = mutableListOf<BlockUiModel.DietFoodItem>()
            val itemsVal = table.get("current_meal_items")
            if (!itemsVal.isnil() && itemsVal.istable()) {
                val itemTable = itemsVal.checktable()
                for (i in 1..itemTable.length()) {
                    val it = itemTable.get(i)
                    if (it.istable()) {
                        currentMealItems.add(BlockUiModel.DietFoodItem(
                            name = it.get("name").optjstring(""),
                            amount = it.get("amount").optjstring("")
                        ))
                    }
                }
            }

            val meals = mutableListOf<BlockUiModel.DietMealItem>()
            val mealsVal = table.get("meals")
            if (!mealsVal.isnil() && mealsVal.istable()) {
                val mTable = mealsVal.checktable()
                for (i in 1..mTable.length()) {
                    val m = mTable.get(i)
                    if (m.istable()) {
                        meals.add(BlockUiModel.DietMealItem(
                            id = m.get("id").optjstring(i.toString()),
                            name = m.get("name").optjstring("Refeição"),
                            time = m.get("time").optjstring(""),
                            calories = m.get("calories").optint(0),
                            isCompleted = m.get("completed").optboolean(false),
                            isCurrent = m.get("current").optboolean(false),
                            commandToExecute = m.get("cmd").optjstring("")
                        ))
                    }
                }
            }

            val days = mutableListOf<BlockUiModel.DietDayItem>()
            val daysVal = table.get("days")
            if (!daysVal.isnil() && daysVal.istable()) {
                val dayTable = daysVal.checktable()
                for (i in 1..dayTable.length()) {
                    val d = dayTable.get(i)
                    if (d.istable()) {
                        days.add(BlockUiModel.DietDayItem(
                            dayId = d.get("day_id").optjstring(""),
                            label = d.get("label").optjstring("Dia"),
                            isCurrent = d.get("is_current").optboolean(false),
                            isToday = d.get("is_today").optboolean(false),
                            commandToExecute = d.get("cmd").optjstring("")
                        ))
                    }
                }
            }

            val files = mutableListOf<BlockUiModel.WorkoutFileItem>()
            val filesVal = table.get("files")
            if (!filesVal.isnil() && filesVal.istable()) {
                val fileTable = filesVal.checktable()
                for (i in 1..fileTable.length()) {
                    val f = fileTable.get(i)
                    if (f.istable()) {
                        files.add(BlockUiModel.WorkoutFileItem(
                            name = f.get("name").optjstring(""),
                            isActive = f.get("is_active").optboolean(false),
                            commandToExecute = f.get("cmd").optjstring("")
                        ))
                    }
                }
            }

            return BlockUiModel.Diet(
                moduleId = id,
                title = blockTitle,
                planName = table.get("plan_name").optjstring("Plano Alimentar"),
                dayName = table.get("day_name").optjstring("Hoje"),
                caloriesConsumed = table.get("calories_consumed").optint(0),
                caloriesTarget = table.get("calories_target").optint(2000),
                proteinConsumed = table.get("protein_consumed").optint(0),
                proteinTarget = table.get("protein_target").optint(150),
                carbsConsumed = table.get("carbs_consumed").optint(0),
                carbsTarget = table.get("carbs_target").optint(200),
                fatsConsumed = table.get("fats_consumed").optint(0),
                fatsTarget = table.get("fats_target").optint(60),
                waterConsumedMl = table.get("water_consumed").optint(0),
                waterTargetMl = table.get("water_target").optint(2500),
                currentMealIndex = table.get("current_meal_index").optint(1),
                totalMeals = table.get("total_meals").optint(1),
                currentMealName = table.get("current_meal_name").optjstring(""),
                currentMealTime = table.get("current_meal_time").optjstring(""),
                currentMealCalories = table.get("current_meal_calories").optint(0),
                currentMealMacros = table.get("current_meal_macros").optjstring(""),
                currentMealNotes = if (table.get("current_meal_notes").isnil()) null else table.get("current_meal_notes").tojstring(),
                currentMealItems = currentMealItems,
                currentMealIsCompleted = table.get("current_meal_completed").optboolean(false),
                checkMealCommand = if (table.get("check_cmd").isnil()) null else table.get("check_cmd").tojstring(),
                nextMealCommand = if (table.get("next_cmd").isnil()) null else table.get("next_cmd").tojstring(),
                prevMealCommand = if (table.get("prev_cmd").isnil()) null else table.get("prev_cmd").tojstring(),
                addWaterCommand250 = table.get("add_water_250_cmd").optjstring("d +250"),
                addWaterCommand500 = table.get("add_water_500_cmd").optjstring("d +500"),
                meals = meals,
                showMealList = table.get("show_meal_list").optboolean(false),
                toggleListCommand = if (table.get("toggle_list_cmd").isnil()) null else table.get("toggle_list_cmd").tojstring(),
                days = days,
                files = files,
                showFileList = table.get("show_file_list").optboolean(false),
                toggleFilesCommand = if (table.get("toggle_files_cmd").isnil()) null else table.get("toggle_files_cmd").tojstring(),
                actions = actions
            )
        }

        // Verifica se é tipo Notes (Anotações e Checklist)
        if (type == "notes") {
            val items = mutableListOf<BlockUiModel.NoteItem>()
            val itemsVal = table.get("items")
            if (!itemsVal.isnil() && itemsVal.istable()) {
                val itemTable = itemsVal.checktable()
                for (i in 1..itemTable.length()) {
                    val it = itemTable.get(i)
                    if (it.istable()) {
                        items.add(BlockUiModel.NoteItem(
                            id = it.get("id").optint(i),
                            text = it.get("text").optjstring(""),
                            isChecklist = it.get("is_checklist").optboolean(false) || it.get("checklist").optboolean(false),
                            isChecked = it.get("is_checked").optboolean(false) || it.get("checked").optboolean(false),
                            toggleCommand = if (it.get("toggle_cmd").isnil()) null else it.get("toggle_cmd").tojstring(),
                            deleteCommand = if (it.get("delete_cmd").isnil()) null else it.get("delete_cmd").tojstring()
                        ))
                    }
                }
            }

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

            val content = if (table.get("content").isnil()) "" else table.get("content").tojstring()

            return BlockUiModel.Notes(
                moduleId = id,
                title = blockTitle,
                subtitle = if (table.get("subtitle").isnil()) null else table.get("subtitle").tojstring(),
                isOpen = table.get("is_open").optboolean(true),
                content = content,
                items = items,
                actions = actions,
                inputHint = if (table.get("input_hint").isnil()) null else table.get("input_hint").tojstring(),
                inputCommand = if (table.get("input_cmd").isnil()) null else table.get("input_cmd").tojstring()
            )
        }

        // Verifica se é tipo Agenda
        if (type == "agenda") {
            val events = mutableListOf<BlockUiModel.AgendaEvent>()
            val eventsVal = table.get("events")
            if (!eventsVal.isnil() && eventsVal.istable()) {
                val eTable = eventsVal.checktable()
                for (i in 1..eTable.length()) {
                    val ev = eTable.get(i)
                    if (ev.istable()) {
                        events.add(BlockUiModel.AgendaEvent(
                            id = ev.get("id").optjstring(i.toString()),
                            title = ev.get("title").optjstring(""),
                            timeLabel = ev.get("time_label").optjstring(""),
                            isAllDay = ev.get("all_day").optboolean(false),
                            location = if (ev.get("location").isnil()) null else ev.get("location").tojstring(),
                            colorHex = if (ev.get("color").isnil()) null else ev.get("color").tojstring(),
                            commandToExecute = if (ev.get("cmd").isnil()) null else ev.get("cmd").tojstring(),
                            deleteCommand = if (ev.get("delete_cmd").isnil()) null else ev.get("delete_cmd").tojstring()
                        ))
                    }
                }
            }

            val tasks = mutableListOf<BlockUiModel.NoteItem>()
            val tasksVal = table.get("tasks")
            if (!tasksVal.isnil() && tasksVal.istable()) {
                val tTable = tasksVal.checktable()
                for (i in 1..tTable.length()) {
                    val it = tTable.get(i)
                    if (it.istable()) {
                        tasks.add(BlockUiModel.NoteItem(
                            id = it.get("id").optint(i),
                            text = it.get("text").optjstring(""),
                            isChecklist = it.get("is_checklist").optboolean(false) || it.get("checklist").optboolean(false),
                            isChecked = it.get("is_checked").optboolean(false) || it.get("checked").optboolean(false),
                            toggleCommand = if (it.get("toggle_cmd").isnil()) null else it.get("toggle_cmd").tojstring(),
                            deleteCommand = if (it.get("delete_cmd").isnil()) null else it.get("delete_cmd").tojstring()
                        ))
                    }
                }
            }

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

            val days = mutableListOf<BlockUiModel.CalendarDay>()
            val daysVal = table.get("days")
            if (!daysVal.isnil() && daysVal.istable()) {
                val dTable = daysVal.checktable()
                for (i in 1..dTable.length()) {
                    val d = dTable.get(i)
                    if (d.istable()) {
                        days.add(BlockUiModel.CalendarDay(
                            dayNumber = d.get("day").optint(-1),
                            isToday = d.get("is_today").optboolean(false),
                            isSelected = d.get("is_selected").optboolean(false),
                            hasEvents = d.get("has_events").optboolean(false),
                            hasTasks = d.get("has_tasks").optboolean(false),
                            dayOfWeekLabel = d.get("wday_label").optjstring(d.get("wday").optjstring("")),
                            fullDateStr = d.get("date_str").optjstring(""),
                            commandToExecute = if (d.get("cmd").isnil()) null else d.get("cmd").tojstring()
                        ))
                    }
                }
            }

            return BlockUiModel.Agenda(
                moduleId = id,
                title = blockTitle,
                subtitle = if (table.get("subtitle").isnil()) null else table.get("subtitle").tojstring(),
                isOpen = table.get("is_open").optboolean(true),
                monthLabel = table.get("month_label").optjstring(""),
                days = days,
                events = events,
                tasks = tasks,
                actions = actions,
                inputHint = if (table.get("input_hint").isnil()) null else table.get("input_hint").tojstring(),
                inputCommand = if (table.get("input_cmd").isnil()) null else table.get("input_cmd").tojstring()
            )
        }

        // Verifica se é tipo Media (Player de música rico)
        if (type == "media" || !table.get("cover").isnil()) {
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
        return synchronized(luaLock) {
            try {
                val trimmed = args.trim()
                if (trimmed == "-h" || trimmed == "--help") {
                    return@synchronized CommandResult.Success("MÓDULO LUA: $name ($commandPrefix)\nAjuda: $helpText")
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
}
