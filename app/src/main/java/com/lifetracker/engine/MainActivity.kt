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

    companion object {
        @Volatile
        private var isInitialized = false

        lateinit var moduleRegistry: ModuleRegistry
            private set
        lateinit var themeModule: ThemeModule
            private set
        lateinit var luaManager: com.lifetracker.engine.modules.lua.LuaModuleManager
            private set

        var currentActivity: MainActivity? = null
            private set
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentActivity = this
        
        val appCtx = applicationContext
        if (!isInitialized) {
            themeModule = ThemeModule(appCtx)
            moduleRegistry = ModuleRegistry(appCtx)
            registerInitialModules(appCtx, themeModule)
            isInitialized = true
        }

        // Imersão Total: Esconde barra de status e navegação para tela inteira real
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            val engineColors by themeModule.currentTheme.collectAsState()

            ModularLifeTrackerTheme(engineColors = engineColors) {
                HomeScreen(moduleRegistry = moduleRegistry)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        currentActivity = this
    }

    private val kioskPrefs by lazy {
        getSharedPreferences("modular_kiosk_prefs", android.content.Context.MODE_PRIVATE)
    }

    var isKioskEnabled: Boolean
        get() = kioskPrefs.getBoolean("kiosk_enabled", false)
        set(value) = kioskPrefs.edit().putBoolean("kiosk_enabled", value).apply()

    override fun onResume() {
        super.onResume()
        currentActivity = this
        
        if (isKioskEnabled) {
            try {
                val dpm = getSystemService(android.content.Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                val adminComponentName = ComponentName(this, AdminReceiver::class.java)

                if (dpm.isDeviceOwnerApp(packageName)) {
                    dpm.setLockTaskPackages(adminComponentName, arrayOf(packageName))
                }
                startLockTask()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        if (currentActivity == this) {
            currentActivity = null
        }
        super.onDestroy()
    }

    private fun registerInitialModules(appCtx: android.content.Context, themeModule: ThemeModule) {
        moduleRegistry.register(themeModule)
        
        val appLauncher = com.lifetracker.engine.modules.system.AppLauncherModule(appCtx) { intent ->
            try {
                if (isKioskEnabled) {
                    currentActivity?.stopLockTask()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            appCtx.startActivity(intent)
        }
        moduleRegistry.register(appLauncher)

        // Gerenciador de Módulos Dinâmicos Lua
        luaManager = com.lifetracker.engine.modules.lua.LuaModuleManager(
            context = appCtx,
            moduleRegistry = moduleRegistry,
            onOpenApp = { query ->
                try {
                    val pm = appCtx.packageManager
                    val intent = pm.getLaunchIntentForPackage(query)
                    if (intent != null) {
                        try {
                            if (isKioskEnabled) currentActivity?.stopLockTask()
                        } catch (_: Exception) {}
                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        appCtx.startActivity(intent)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        )
        luaManager.init()

        moduleRegistry.register(com.lifetracker.engine.modules.system.SysCtlModule(
            context = appCtx,
            onBrightnessChange = { brightness ->
                currentActivity?.let { act ->
                    act.runOnUiThread {
                        val layoutParams = act.window.attributes
                        layoutParams.screenBrightness = brightness.coerceIn(0.01f, 1f)
                        act.window.attributes = layoutParams
                    }
                }
            },
            onPin = {
                isKioskEnabled = true
                currentActivity?.runOnUiThread {
                    try {
                        val dpm = getSystemService(android.content.Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                        val adminComponentName = ComponentName(this@MainActivity, AdminReceiver::class.java)
                        if (dpm.isDeviceOwnerApp(packageName)) {
                            dpm.setLockTaskPackages(adminComponentName, arrayOf(packageName))
                        }
                        currentActivity?.startLockTask()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            },
            onUnpin = {
                isKioskEnabled = false
                currentActivity?.runOnUiThread {
                    try {
                        currentActivity?.stopLockTask()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
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
