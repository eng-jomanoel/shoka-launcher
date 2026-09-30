package com.lifetracker.engine.core.module

import com.lifetracker.engine.core.model.BlockUiModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

/**
 * Registro central dos módulos instalados na Engine.
 * Responsável por gerenciar os plugins e rotear os comandos do Terminal CLI.
 * Executa as operações de state combinadas em background para evitar sobrecarga na UI.
 */
class ModuleRegistry(
    private val context: android.content.Context? = null,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val prefs by lazy {
        context?.getSharedPreferences("modular_engine_prefs", android.content.Context.MODE_PRIVATE)
    }

    private val _modules = MutableStateFlow<List<EngineModule>>(emptyList())
    val modules: StateFlow<List<EngineModule>> = _modules.asStateFlow()

    private val _moduleOrder = MutableStateFlow<List<String>>(loadSavedOrder())
    val moduleOrder: StateFlow<List<String>> = _moduleOrder.asStateFlow()

    private val _hiddenModules = MutableStateFlow<Set<String>>(loadSavedHidden())
    val hiddenModules: StateFlow<Set<String>> = _hiddenModules.asStateFlow()

    val isEditMode = MutableStateFlow(false)

    // Otimização: Mescla os flows de todos os módulos respeitando a ordem e ocultação
    @OptIn(ExperimentalCoroutinesApi::class)
    val activeBlocksFlow: StateFlow<List<BlockUiModel>> = combine(
        _modules,
        _moduleOrder,
        _hiddenModules
    ) { moduleList, order, hidden ->
        val visible = moduleList.filterNot { it.id in hidden }
        visible.sortedBy { mod ->
            val idx = order.indexOf(mod.id)
            if (idx == -1) 999 else idx
        }
    }.flatMapLatest { sortedList ->
        if (sortedList.isEmpty()) return@flatMapLatest flowOf(emptyList())
        val flowList = sortedList.map { it.blockFlow }
        combine(flowList) { blockArray ->
            blockArray.filterNotNull()
        }
    }.stateIn(
        scope = coroutineScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun register(module: EngineModule) {
        val current = _modules.value.toMutableList()
        current.removeAll { it.id == module.id }
        current.add(module)
        _modules.value = current

        // Se o módulo não estiver na lista de ordem, adiciona no final
        if (!_moduleOrder.value.contains(module.id)) {
            val updatedOrder = _moduleOrder.value.toMutableList()
            updatedOrder.add(module.id)
            _moduleOrder.value = updatedOrder
            saveOrder(updatedOrder)
        }
    }

    fun unregister(moduleId: String) {
        _modules.value = _modules.value.filterNot { it.id == moduleId }
    }

    fun toggleEditMode(): Boolean {
        isEditMode.value = !isEditMode.value
        return isEditMode.value
    }

    fun moveUp(moduleId: String): Boolean {
        val current = _moduleOrder.value.toMutableList()
        val index = current.indexOf(moduleId)
        if (index > 0) {
            val temp = current[index]
            current[index] = current[index - 1]
            current[index - 1] = temp
            _moduleOrder.value = current
            saveOrder(current)
            return true
        }
        return false
    }

    fun moveDown(moduleId: String): Boolean {
        val current = _moduleOrder.value.toMutableList()
        val index = current.indexOf(moduleId)
        if (index in 0 until current.size - 1) {
            val temp = current[index]
            current[index] = current[index + 1]
            current[index + 1] = temp
            _moduleOrder.value = current
            saveOrder(current)
            return true
        }
        return false
    }

    fun moveToPosition(moduleId: String, position: Int): Boolean {
        val current = _moduleOrder.value.toMutableList()
        val index = current.indexOf(moduleId)
        val targetIndex = (position - 1).coerceIn(0, current.size - 1)
        if (index != -1 && index != targetIndex) {
            val item = current.removeAt(index)
            current.add(targetIndex, item)
            _moduleOrder.value = current
            saveOrder(current)
            return true
        }
        return false
    }

    fun hideModule(moduleId: String) {
        val set = _hiddenModules.value.toMutableSet()
        set.add(moduleId)
        _hiddenModules.value = set
        saveHidden(set)
    }

    fun showModule(moduleId: String) {
        val set = _hiddenModules.value.toMutableSet()
        set.remove(moduleId)
        _hiddenModules.value = set
        saveHidden(set)
    }

    private fun loadSavedOrder(): List<String> {
        val raw = prefs?.getString("module_order", null) ?: return emptyList()
        return raw.split(",").filter { it.isNotBlank() }
    }

    private fun saveOrder(order: List<String>) {
        prefs?.edit()?.putString("module_order", order.joinToString(","))?.apply()
    }

    private fun loadSavedHidden(): Set<String> {
        return prefs?.getStringSet("hidden_modules", emptySet()) ?: emptySet()
    }

    private fun saveHidden(hidden: Set<String>) {
        prefs?.edit()?.putStringSet("hidden_modules", hidden)?.apply()
    }

    suspend fun dispatch(input: String): CommandResult {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return CommandResult.Ignored

        val parts = trimmed.split("\\s+".toRegex(), limit = 2)
        val prefix = parts[0].lowercase()
        val args = if (parts.size > 1) parts[1] else ""

        if (prefix == "help") {
            val helpList = _modules.value.joinToString("\n") { mod ->
                "[${mod.commandPrefix}] ${mod.helpText}"
            }
            return CommandResult.Success("Comandos disponíveis:\n$helpList")
        }

        val targetModule = _modules.value.firstOrNull { 
            it.commandPrefix.equals(prefix, ignoreCase = true) 
        }

        return if (targetModule != null) {
            targetModule.executeCommand(args)
        } else {
            CommandResult.Error("Comando '$prefix' desconhecido. Digite 'help'.")
        }
    }
}
