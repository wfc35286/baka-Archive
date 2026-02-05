package com.wfc.hook.oplus.games

import android.os.Bundle
import android.os.Build
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.content.Context
import android.content.SharedPreferences
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState

import com.highcapable.yukihookapi.YukiHookAPI

import com.wfc.hook.oplus.games.UI.Home.HomeScreen
import com.wfc.hook.oplus.games.UI.About.AboutScreen
import com.wfc.hook.oplus.games.UI.SettingsScreen
import com.wfc.hook.oplus.games.UI.BonusGalleryScreen 
import com.wfc.hook.oplus.games.UI.About.UpdateDialog
import com.wfc.hook.oplus.games.UI.ShopScreen 

enum class CheckState {
    Checking, NoRoot, NoLSPosed, Success
}

object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
    const val SHOP = "shop"   
    const val BONUS = "bonus"
}

data class ThemePreset(val color: Color)

class MainActivity : ComponentActivity() {
    companion object {
        fun applyStatusBarSystemUi(activity: android.app.Activity?, isHidden: Boolean) {
            if (activity == null) return
            val window = activity.window
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            if (isHidden) {
                insetsController.hide(WindowInsetsCompat.Type.statusBars())
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                insetsController.show(WindowInsetsCompat.Type.statusBars())
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        createUpdateChannelHelper(this)
        
        enableEdgeToEdge()
        setContent {
            GameHelperTheme {
                AppContent()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameHelperTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("ui_settings", Context.MODE_PRIVATE) }
    val THEME_ANIM_TIME = 1000
    var tick by remember { mutableIntStateOf(0) }
    
    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> tick++ }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    val darkModeStrategy = key(tick) { prefs.getInt("dark_mode_strategy", 0) }
    val isOledBlackEnabled = key(tick) { prefs.getBoolean("oled_black", false) }
    val useDynamicColor = key(tick) { prefs.getBoolean("dynamic_color", Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) }
    val isUsingCustom = key(tick) { prefs.getBoolean("is_using_custom", false) }
    val colorIndex = key(tick) { prefs.getInt("theme_color_index", 1) } 
    val customColorArgb = key(tick) { prefs.getInt("custom_theme_color", Color(0xFFF7A8B8).toArgb()) }
    val hideStatusBar = key(tick) { prefs.getBoolean("hide_status_bar", false) }

    val isDarkTheme = when (darkModeStrategy) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    
    val themePresets = listOf(ThemePreset(Color(0xFF0D47A1)), ThemePreset(Color(0xFFFFB7C5)), ThemePreset(Color(0xFFFFF4E0)), ThemePreset(Color(0xFF1B5E20)), ThemePreset(Color(0xFFFF6D00)), ThemePreset(Color(0xFF4A148C)), ThemePreset(Color(0xFF121212)), ThemePreset(Color(0xFF00838F)))
    
    val targetColorScheme = remember(tick, isDarkTheme, isOledBlackEnabled, useDynamicColor, colorIndex, isUsingCustom, customColorArgb) {
        if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (isDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            val seedColor = if (isUsingCustom) Color(customColorArgb) else themePresets.getOrElse(colorIndex) { themePresets[1] }.color
            ColorPresets.generateMonetStandardScheme(seedColor, isDarkTheme, isOledBlackEnabled)
        }
    }

    val colorSpec = tween<Color>(durationMillis = THEME_ANIM_TIME)
    val p by animateColorAsState(targetColorScheme.primary, colorSpec, label = "p")
    val op by animateColorAsState(targetColorScheme.onPrimary, colorSpec, label = "op")
    val pc by animateColorAsState(targetColorScheme.primaryContainer, colorSpec, label = "pc")
    val opc by animateColorAsState(targetColorScheme.onPrimaryContainer, colorSpec, label = "opc")
    val s by animateColorAsState(targetColorScheme.secondary, colorSpec, label = "s")
    val os by animateColorAsState(targetColorScheme.onSecondary, colorSpec, label = "os")
    val sc by animateColorAsState(targetColorScheme.secondaryContainer, colorSpec, label = "sc")
    val osc by animateColorAsState(targetColorScheme.onSecondaryContainer, colorSpec, label = "osc")
    val t by animateColorAsState(targetColorScheme.tertiary, colorSpec, label = "t")
    val ot by animateColorAsState(targetColorScheme.onTertiary, colorSpec, label = "ot")
    val tc by animateColorAsState(targetColorScheme.tertiaryContainer, colorSpec, label = "tc")
    val otc by animateColorAsState(targetColorScheme.onTertiaryContainer, colorSpec, label = "otc")
    val bg by animateColorAsState(targetColorScheme.background, colorSpec, label = "bg")
    val surf by animateColorAsState(targetColorScheme.surface, colorSpec, label = "surf")
    val obg by animateColorAsState(targetColorScheme.onBackground, colorSpec, label = "obg")
    val osurf by animateColorAsState(targetColorScheme.onSurface, colorSpec, label = "osurf")
    val sv by animateColorAsState(targetColorScheme.surfaceVariant, colorSpec, label = "sv")
    val osv by animateColorAsState(targetColorScheme.onSurfaceVariant, colorSpec, label = "osv")
    val outline by animateColorAsState(targetColorScheme.outline, colorSpec, label = "out")
    val err by animateColorAsState(targetColorScheme.error, colorSpec, label = "err")

    val animatedColorScheme = targetColorScheme.copy(
        primary = p, onPrimary = op, primaryContainer = pc, onPrimaryContainer = opc,
        secondary = s, onSecondary = os, secondaryContainer = sc, onSecondaryContainer = osc,
        tertiary = t, onTertiary = ot, tertiaryContainer = tc, onTertiaryContainer = otc,
        background = bg, surface = surf, onBackground = obg, onSurface = osurf,
        surfaceVariant = sv, onSurfaceVariant = osv, outline = outline, error = err
    )

    LaunchedEffect(hideStatusBar) {
        MainActivity.applyStatusBarSystemUi(context.findActivityHelper(), hideStatusBar)
    }

    @Suppress("DEPRECATION")
    CompositionLocalProvider(LocalRippleConfiguration provides null) {
        MaterialTheme(colorScheme = animatedColorScheme) {
            Surface(modifier = Modifier.fillMaxSize(), color = animatedColorScheme.background) {
                content()
            }
        }
    }
}

object ColorPresets {
    fun generateMonetStandardScheme(seed: Color, isDark: Boolean, isOled: Boolean): ColorScheme {
        val sArgb = seed.toArgb()
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(sArgb, hsl)
        val hue = hsl[0]
        val saturation = hsl[1]
        val secondaryHue = hue
        val secondarySat = (saturation * 0.35f).coerceIn(0.1f, 0.4f)
        val tertiaryHue = (hue + 60f) % 360f
        val tertiarySat = (saturation * 0.5f).coerceIn(0.2f, 0.6f)

        return if (isDark) {
            darkColorScheme(
                primary = fromHsl(hue, saturation.coerceAtLeast(0.4f), 0.75f),
                onPrimary = fromHsl(hue, saturation, 0.15f),
                primaryContainer = fromHsl(hue, saturation.coerceIn(0.2f, 0.5f), 0.25f),
                onPrimaryContainer = fromHsl(hue, saturation, 0.85f),
                secondary = fromHsl(secondaryHue, secondarySat, 0.7f),
                onSecondary = fromHsl(secondaryHue, secondarySat, 0.2f),
                secondaryContainer = fromHsl(secondaryHue, secondarySat, 0.3f),
                onSecondaryContainer = fromHsl(secondaryHue, secondarySat, 0.9f),
                tertiary = fromHsl(tertiaryHue, tertiarySat, 0.75f),
                onTertiary = fromHsl(tertiaryHue, tertiarySat, 0.2f),
                tertiaryContainer = fromHsl(tertiaryHue, tertiarySat, 0.3f),
                onTertiaryContainer = fromHsl(tertiaryHue, tertiarySat, 0.9f),
                background = if (isOled) Color.Black else fromHsl(hue, saturation * 0.1f, 0.12f).blend(Color(0xFF1A1C1E), 0.6f),
                surface = if (isOled) Color(0xFF080808) else fromHsl(hue, saturation * 0.1f, 0.14f).blend(Color(0xFF202225), 0.6f),
                onBackground = Color(0xFFE2E2E6),
                onSurface = Color(0xFFE2E2E6),
                surfaceVariant = fromHsl(hue, saturation * 0.15f, 0.28f),
                onSurfaceVariant = Color(0xFFC4C6D0),
                outline = fromHsl(hue, saturation * 0.15f, 0.55f)
            )
        } else {
            lightColorScheme(
                primary = fromHsl(hue, saturation, 0.4f),
                onPrimary = Color.White,
                primaryContainer = fromHsl(hue, saturation, 0.9f),
                onPrimaryContainer = fromHsl(hue, saturation, 0.1f),
                secondary = fromHsl(secondaryHue, secondarySat, 0.45f),
                onSecondary = Color.White,
                secondaryContainer = fromHsl(secondaryHue, secondarySat, 0.92f),
                onSecondaryContainer = fromHsl(secondaryHue, secondarySat, 0.15f),
                tertiary = fromHsl(tertiaryHue, tertiarySat, 0.45f),
                onTertiary = Color.White,
                tertiaryContainer = fromHsl(tertiaryHue, tertiarySat, 0.92f),
                onTertiaryContainer = fromHsl(tertiaryHue, tertiarySat, 0.15f),
                background = fromHsl(hue, saturation * 0.05f, 0.98f),
                surface = fromHsl(hue, saturation * 0.05f, 0.96f),
                onBackground = Color(0xFF1B1B1F),
                onSurface = Color(0xFF1B1B1F),
                surfaceVariant = fromHsl(hue, saturation * 0.12f, 0.92f),
                onSurfaceVariant = Color(0xFF44474F),
                outline = fromHsl(hue, saturation * 0.15f, 0.45f)
            )
        }
    }

    private fun fromHsl(h: Float, s: Float, l: Float): Color {
        return Color(ColorUtils.HSLToColor(floatArrayOf(h, s, l)))
    }

    private fun Color.blend(other: Color, ratio: Float): Color {
        return Color(ColorUtils.blendARGB(this.toArgb(), other.toArgb(), ratio))
    }
}

@Composable
fun AppContent() {
    val context = LocalContext.current
    var checkState by remember { mutableStateOf(CheckState.Checking) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        withContext(Dispatchers.IO) {
            // 模拟加载延时，保持启动动画的平滑
            delay(1200)
            
            // 1. Root 权限检测
            val hasRoot = RootUtils.checkRoot()
            // 2. YukiHookAPI (LSPosed) 模块激活状态检测
            val isModuleActive = YukiHookAPI.Status.isModuleActive
            
            withContext(Dispatchers.Main) {
                checkState = when {
                    !hasRoot -> CheckState.NoRoot
                    !isModuleActive -> CheckState.NoLSPosed
                    else -> CheckState.Success
                }
                
                // 只有检测全部通过后，才进行自动更新检查
                if (checkState == CheckState.Success) {
                    val prefs = context.getSharedPreferences("ui_settings", Context.MODE_PRIVATE)
                    if (prefs.getBoolean("auto_check_update", true)) {
                        checkRemoteUpdate(context) { showUpdateDialog = true }
                    }
                }
            }
        }
    }

    if (showUpdateDialog) {
        UpdateDialog(onDismiss = { showUpdateDialog = false })
    }

    AnimatedContent(
        targetState = checkState,
        transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
        label = "root_check"
    ) { state ->
        when (state) {
            CheckState.Checking -> CheckingScreen()
            CheckState.NoRoot -> ErrorScreen("未检测到 Root 权限", "本模块需要 Root 权限以读写配置，请授予权限")
            CheckState.NoLSPosed -> ErrorScreen("模块未激活", "未检测到模块激活，请确认 LSPosed 状态")
            CheckState.Success -> MainScreen()
        }
    }
}

// ... CheckingScreen, ErrorScreen (保持不变) ...

@Composable
fun CheckingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorScreen(title: String, message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                imageVector = Icons.Filled.Warning, 
                contentDescription = null, 
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.error)
            Text(message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AnimatedVisibility(
                visible = currentRoute != Routes.BONUS,
                enter = slideInVertically(tween(300)) { it },
                exit = slideOutVertically(tween(300)) { it }
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 4.dp,
                    windowInsets = WindowInsets.navigationBars 
                ) {
                    val items = listOf(
                        Routes.HOME to ("首页" to (Icons.Filled.Home to Icons.Outlined.Home)),
                        Routes.SHOP to ("商店" to (Icons.Filled.ShoppingBag to Icons.Outlined.ShoppingBag)),
                        Routes.SETTINGS to ("设置" to (Icons.Filled.Settings to Icons.Outlined.Settings)),
                        Routes.ABOUT to ("关于" to (Icons.Filled.Info to Icons.Outlined.Info))
                    )
                    items.forEach { (route, content) ->
                        val (label, icons) = content
                        val isSelected = currentRoute == route
                        NavigationBarItem(
                            icon = { 
                                Icon(
                                    if (isSelected) icons.first else icons.second, 
                                    contentDescription = label,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                ) 
                            },
                            label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            alwaysShowLabel = false, 
                            selected = isSelected,
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            onClick = {
                                if (currentRoute != route) {
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Routes.HOME) { HomeScreen() }
                composable(Routes.SETTINGS) { SettingsScreen() }
                composable(Routes.ABOUT) {
                    AboutScreen(
                        onNavigateToBonus = { navController.navigate(Routes.BONUS) },
                        onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                        onNavigateToUpdate = { },
                        onBackToHome = { navController.navigate(Routes.HOME) }
                    )
                }
                composable(Routes.SHOP) {
                    ShopScreen(onBack = { 
                        navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
                    })
                }
                composable(Routes.BONUS) {
                    BonusGalleryScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}

// ⭐⭐ 所有的工具函数都放在这里，作为顶层函数 ⭐⭐

fun Context.findActivityHelper(): android.app.Activity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is android.app.Activity) return context
        context = context.baseContext
    }
    return null
}

fun createUpdateChannelHelper(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = android.app.NotificationChannel(
            "update_channel", "版本更新", android.app.NotificationManager.IMPORTANCE_LOW
        )
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.createNotificationChannel(channel)
    }
}

// 修正后的 checkRemoteUpdate (suspend 函数)
suspend fun checkRemoteUpdate(context: Context, onNewVersion: () -> Unit) {
    withContext(Dispatchers.IO) {
        try {
            val currentLocalVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
            val url = URL("https://api.github.com/repos/wfc35286/baka-update/releases/latest")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "Roxy-IDE")
                connectTimeout = 5000
            }
            if (conn.responseCode == 200) {
                val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                val remoteTag = json.getString("tag_name")
                if (checkVersionUpdate(currentLocalVersion, remoteTag)) {
                    withContext(Dispatchers.Main) { onNewVersion() }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }
}

fun checkVersionUpdate(local: String, remote: String): Boolean {
    return try {
        val l = local.replace("v", "", ignoreCase = true).split(".").map { it.toIntOrNull() ?: 0 }
        val r = remote.replace("v", "", ignoreCase = true).split(".").map { it.toIntOrNull() ?: 0 }
        
        for (i in 0 until maxOf(l.size, r.size)) {
            val lv = l.getOrElse(i) { 0 }
            val rv = r.getOrElse(i) { 0 }
            if (rv > lv) return true
            if (rv < lv) return false
        }
        false
    } catch (e: Exception) {
        false
    }
}
