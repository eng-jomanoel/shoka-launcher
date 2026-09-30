package com.lifetracker.engine

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.lifetracker.engine.core.module.ModuleRegistry
import com.lifetracker.engine.core.module.ThemeModule
import com.lifetracker.engine.receiver.AdminReceiver
import com.lifetracker.engine.ui.screens.HomeScreen
import com.lifetracker.engine.ui.theme.ModularLifeTrackerTheme

class MainActivity : ComponentActivity() {

    private val moduleRegistry by lazy { ModuleRegistry(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val themeModule = ThemeModule(applicationContext)

        // Imersão Total: Esconde barra de status e navegação para tela inteira real
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        registerInitialModules(themeModule)

        setContent {
            val engineColors by themeModule.currentTheme.collectAsState()

            ModularLifeTrackerTheme(engineColors = engineColors) {
                HomeScreen(moduleRegistry = moduleRegistry)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        
        try {
            val dpm = getSystemService(android.content.Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val adminComponentName = ComponentName(this, AdminReceiver::class.java)

            if (dpm.isDeviceOwnerApp(packageName)) {
                // Modo Kiosk Silencioso e Absoluto (Sem pop-up chato)
                dpm.setLockTaskPackages(adminComponentName, arrayOf(packageName))
                startLockTask()
            } else {
                // Modo Kiosk Normal (Com pop-up de segurança pedindo 'OK')
                // Se a pessoa desativar a opção B, é só comentar o startLockTask() aqui.
                startLockTask()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun registerInitialModules(themeModule: ThemeModule) {
        moduleRegistry.register(themeModule)
        
        val appLauncher = com.lifetracker.engine.modules.system.AppLauncherModule(applicationContext) { intent ->
            try {
                // Remove o pin temporariamente para permitir que outro app venha para a frente
                stopLockTask()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            startActivity(intent)
        }
        moduleRegistry.register(appLauncher)

        // Gerenciador de Módulos Dinâmicos Lua
        val luaManager = com.lifetracker.engine.modules.lua.LuaModuleManager(
            context = applicationContext,
            moduleRegistry = moduleRegistry,
            onOpenApp = { query ->
                // Permite que scripts Lua abram apps do sistema
                try {
                    val pm = applicationContext.packageManager
                    val intent = pm.getLaunchIntentForPackage(query)
                    if (intent != null) {
                        try { stopLockTask() } catch (_: Exception) {}
                        startActivity(intent)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        )
        luaManager.init()

        moduleRegistry.register(com.lifetracker.engine.modules.system.SysCtlModule(
            context = applicationContext,
            onBrightnessChange = { brightness ->
                // Modifica o brilho da janela da Activity
                val layoutParams = window.attributes
                // O Android espera um valor de 0.0 a 1.0. Menos que 0 retorna ao brilho automático do sistema.
                layoutParams.screenBrightness = brightness.coerceIn(0.01f, 1f)
                window.attributes = layoutParams
            },
            onUnpin = {
                try {
                    stopLockTask()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            onReload = {
                luaManager.reloadAll()
            }
        ))

        // Módulo de Gerenciamento da Home (mod list, mod move, mod hide, mod edit)
        moduleRegistry.register(com.lifetracker.engine.modules.system.ModuleManagementModule(moduleRegistry))
    }
}
