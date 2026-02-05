package com.wfc.hook.oplus.games.UI

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import android.view.View
import android.view.WindowInsets as ViewWindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize 
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.wfc.hook.oplus.games.MainActivity

// --- 1. 数据模型 ---
data class PresetColor(val name: String, val color: Color, val desc: String)

// --- 2. 工具函数与组件 ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisableRippleTheme(content: @Composable () -> Unit) {
    @Suppress("DEPRECATION")
    CompositionLocalProvider(LocalRippleConfiguration provides null, content = content)
}

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

fun createUpdateNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel("UPDATE_CHANNEL", "版本更新下载", NotificationManager.IMPORTANCE_LOW)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}

@Composable
fun PixelDivider(color: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 4.dp)
            .height(1.dp)
            .background(color)
    )
}

fun Color.adaptiveContentColor(): Color {
    return if (this.luminance() > 0.6f) Color(0xFF1A1C1E) else Color.White
}

// --- 3. 设置界面 ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("ui_settings", Context.MODE_PRIVATE) }
    val scrollState = rememberScrollState()

    val themePresets = listOf(
        PresetColor("深海蓝", Color(0xFF0D47A1), "深邃稳重的科技蓝色调"),
        PresetColor("樱花粉", Color(0xFFFFB7C5), "柔和舒适的淡粉色视觉"),
        PresetColor("奶油米白", Color(0xFFFFF4E0), "温润自然的低饱和度色彩"),
        PresetColor("森林绿", Color(0xFF1B5E20), "护眼、高对比度的深绿色"),
        PresetColor("活力橙", Color(0xFFFF6D00), "明亮醒目的橙色预设"),
        PresetColor("星空紫", Color(0xFF4A148C), "优雅神秘的紫色主题"),
        PresetColor("极简黑", Color(0xFF121212), "低光环境下推荐使用的纯黑"),
        PresetColor("湖水青", Color(0xFF00838F), "清新淡雅的青色预设方案")
    )

    var darkModeStrategy by remember { mutableIntStateOf(prefs.getInt("dark_mode_strategy", 0)) }
    var useDynamicColor by remember { mutableStateOf(prefs.getBoolean("dynamic_color", Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)) }
    var isUsingCustom by remember { mutableStateOf(prefs.getBoolean("is_using_custom", false)) }
    var selectedColorIndex by remember { mutableIntStateOf(prefs.getInt("theme_color_index", 1)) }
    var customColorArgb by remember { mutableIntStateOf(prefs.getInt("custom_theme_color", Color(0xFFF7A8B8).toArgb())) }
    var isOledBlack by remember { mutableStateOf(prefs.getBoolean("oled_black", false)) }
    var hideStatusBar by remember { mutableStateOf(prefs.getBoolean("hide_status_bar", false)) }
    var useBlurEffect by remember { mutableStateOf(prefs.getBoolean("use_blur_effect", true)) }
    var autoCheckUpdate by remember { mutableStateOf(prefs.getBoolean("auto_check_update", true)) }

    var colorExpanded by remember { mutableStateOf(true) }      
    var visualExpanded by remember { mutableStateOf(true) }    
    var syncExpanded by remember { mutableStateOf(true) }     
    var presetsExpanded by remember { mutableStateOf(false) }
    var showRGBCustomizer by remember { mutableStateOf(false) }

    val smoothSpringFloat: FiniteAnimationSpec<Float> = spring(stiffness = Spring.StiffnessMediumLow)
    val smoothSpringSize: FiniteAnimationSpec<IntSize> = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioLowBouncy)
    
    val currentColorScheme = MaterialTheme.colorScheme

    if (showRGBCustomizer) {
        RGBSliderDialog(
            initialColor = Color(customColorArgb),
            onDismiss = { showRGBCustomizer = false },
            onConfirm = { color ->
                customColorArgb = color.toArgb()
                isUsingCustom = true
                prefs.edit().putInt("custom_theme_color", customColorArgb).putBoolean("is_using_custom", true).apply()
                showRGBCustomizer = false
            }
        )
    }

    DisableRippleTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            containerColor = currentColorScheme.background
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // --- 顶部文字标题 ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = currentColorScheme.primary, modifier = Modifier.size(6.dp)) {}
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "偏好设定", 
                            style = MaterialTheme.typography.labelLarge, 
                            color = currentColorScheme.primary, 
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "应用个性化设置", 
                        style = MaterialTheme.typography.headlineLarge, 
                        fontWeight = FontWeight.Black,
                        color = currentColorScheme.onSurface
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // 1. 主题色彩设置
                    SettingsSection(
                        title = "主题色彩设置", 
                        icon = Icons.Filled.ColorLens, 
                        isExpanded = colorExpanded, 
                        onToggle = { colorExpanded = !colorExpanded },
                        animationSpecSize = smoothSpringSize,
                        animationSpecFloat = smoothSpringFloat,
                        // 洛琪希优化：边框跟随主题色
                        outlineColor = currentColorScheme.primary.copy(alpha = 0.35f),
                        primaryColor = currentColorScheme.primary
                    ) {
                        SettingsToggleItem(
                            "动态色彩 (Android 12+)", 
                            "基于壁纸色调自动生成系统调色板", 
                            Icons.Filled.AutoAwesome, 
                            useDynamicColor, 
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                            textColor = currentColorScheme.onSurface,
                            subTextColor = currentColorScheme.onSurfaceVariant,
                            primaryColor = currentColorScheme.primary
                        ) { 
                            useDynamicColor = it; prefs.edit().putBoolean("dynamic_color", it).apply() 
                        }

                        AnimatedVisibility(
                            visible = !useDynamicColor,
                            enter = expandVertically(smoothSpringSize) + fadeIn(smoothSpringFloat),
                            exit = shrinkVertically(smoothSpringSize) + fadeOut(smoothSpringFloat)
                        ) {
                            Column {
                                PixelDivider(currentColorScheme.outlineVariant.copy(alpha = 0.3f))
                                OutlinedButton(
                                    onClick = { showRGBCustomizer = true },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, if(isUsingCustom) currentColorScheme.primary else currentColorScheme.outline.copy(alpha = 0.5f))
                                ) {
                                    Icon(Icons.Filled.Tune, null, modifier = Modifier.size(18.dp), tint = currentColorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Text("高级自定义调色盘", color = currentColorScheme.primary)
                                    Spacer(Modifier.weight(1f))
                                    Box(Modifier.size(18.dp).clip(CircleShape).background(Color(customColorArgb)).border(1.dp, currentColorScheme.outline, CircleShape))
                                }

                                val containerBaseColor = if (isUsingCustom) Color(customColorArgb) else themePresets[selectedColorIndex].color
                                val adaptiveIconColor = containerBaseColor.adaptiveContentColor()

                                Surface(
                                    modifier = Modifier.fillMaxWidth().clickable { presetsExpanded = !presetsExpanded },
                                    shape = RoundedCornerShape(14.dp),
                                    color = containerBaseColor.copy(alpha = 0.15f),
                                    border = BorderStroke(1.2.dp, containerBaseColor.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.animateContentSize(smoothSpringSize)) {
                                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Palette, null, modifier = Modifier.size(20.dp), tint = adaptiveIconColor)
                                            Spacer(Modifier.width(12.dp))
                                            Text("预设颜色方案: ${if(isUsingCustom) "自定义" else themePresets[selectedColorIndex].name}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = adaptiveIconColor)
                                            Spacer(Modifier.weight(1f))
                                            Icon(if(presetsExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, tint = adaptiveIconColor)
                                        }
                                        
                                        AnimatedVisibility(
                                            visible = presetsExpanded,
                                            enter = expandVertically(smoothSpringSize),
                                            exit = shrinkVertically(smoothSpringSize)
                                        ) {
                                            Column {
                                                themePresets.forEachIndexed { index, preset ->
                                                    PixelDivider(currentColorScheme.outlineVariant.copy(alpha = 0.3f))
                                                    PresetItemView(
                                                        preset, 
                                                        !isUsingCustom && selectedColorIndex == index,
                                                        textColor = currentColorScheme.onSurface,
                                                        subTextColor = currentColorScheme.onSurfaceVariant
                                                    ) {
                                                        selectedColorIndex = index
                                                        isUsingCustom = false
                                                        prefs.edit().putInt("theme_color_index", index).putBoolean("is_using_custom", false).apply()
                                                        scope.launch { delay(150); presetsExpanded = false }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. 显示与交互
                    SettingsSection(
                        title = "显示与交互", 
                        icon = Icons.Filled.AutoFixHigh, 
                        isExpanded = visualExpanded, 
                        onToggle = { visualExpanded = !visualExpanded },
                        animationSpecSize = smoothSpringSize,
                        animationSpecFloat = smoothSpringFloat,
                        outlineColor = currentColorScheme.primary.copy(alpha = 0.35f),
                        primaryColor = currentColorScheme.primary
                    ) {
                        val options = listOf("跟随系统", "浅色", "深色")
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            options.forEachIndexed { index, label ->
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                                    onClick = { darkModeStrategy = index; prefs.edit().putInt("dark_mode_strategy", index).apply() },
                                    selected = darkModeStrategy == index,
                                    colors = SegmentedButtonDefaults.colors(
                                        activeContainerColor = currentColorScheme.primary,
                                        activeContentColor = currentColorScheme.surface,
                                        inactiveContainerColor = Color.Transparent,
                                        inactiveContentColor = currentColorScheme.onSurface,
                                        activeBorderColor = currentColorScheme.outline,
                                        inactiveBorderColor = currentColorScheme.outline
                                    )
                                ) { Text(label) }
                            }
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        PixelDivider(currentColorScheme.outlineVariant.copy(alpha = 0.3f))
                        
                        SettingsToggleItem(
                            "实时高斯模糊", "为顶栏和浮窗提供磨砂玻璃质感", Icons.Filled.GraphicEq, useBlurEffect,
                            textColor = currentColorScheme.onSurface, subTextColor = currentColorScheme.onSurfaceVariant,
                            primaryColor = currentColorScheme.primary
                        ) {
                            useBlurEffect = it; prefs.edit().putBoolean("use_blur_effect", it).apply()
                        }

                        PixelDivider(currentColorScheme.outlineVariant.copy(alpha = 0.3f))

                        SettingsToggleItem(
                            "全屏沉浸显示", "深度适配并隐藏系统状态栏", Icons.Filled.FitScreen, hideStatusBar,
                            textColor = currentColorScheme.onSurface, subTextColor = currentColorScheme.onSurfaceVariant,
                            primaryColor = currentColorScheme.primary
                        ) {
                            hideStatusBar = it
                            prefs.edit().putBoolean("hide_status_bar", it).apply()
                            MainActivity.applyStatusBarSystemUi(context.findActivity(), it)
                        }

                        val isActuallyDark = when (darkModeStrategy) { 
                            1 -> false 
                            2 -> true 
                            else -> isSystemInDarkTheme() 
                        }

                        AnimatedVisibility(
                            visible = isActuallyDark && !useDynamicColor,
                            enter = expandVertically(smoothSpringSize) + fadeIn(smoothSpringFloat),
                            exit = shrinkVertically(smoothSpringSize) + fadeOut(smoothSpringFloat)
                        ) {
                            Column {
                                PixelDivider(currentColorScheme.outlineVariant.copy(alpha = 0.3f))
                                SettingsToggleItem(
                                    "OLED 纯黑模式", "关闭像素点以获得极致对比度", Icons.Filled.Contrast, isOledBlack,
                                    textColor = currentColorScheme.onSurface, subTextColor = currentColorScheme.onSurfaceVariant,
                                    primaryColor = currentColorScheme.primary
                                ) {
                                    isOledBlack = it; prefs.edit().putBoolean("oled_black", it).apply()
                                }
                            }
                        }
                    }

                    // 3. 通用设置
                    SettingsSection(
                        title = "通用设置", 
                        icon = Icons.Filled.SettingsSuggest, 
                        isExpanded = syncExpanded, 
                        onToggle = { syncExpanded = !syncExpanded },
                        animationSpecSize = smoothSpringSize,
                        animationSpecFloat = smoothSpringFloat,
                        outlineColor = currentColorScheme.primary.copy(alpha = 0.35f),
                        primaryColor = currentColorScheme.primary
                    ) {
                        SettingsToggleItem(
                            "启动检查更新", "保持应用版本始终为最新状态", Icons.Filled.CloudSync, autoCheckUpdate,
                            textColor = currentColorScheme.onSurface, subTextColor = currentColorScheme.onSurfaceVariant,
                            primaryColor = currentColorScheme.primary
                        ) { 
                            autoCheckUpdate = it; prefs.edit().putBoolean("auto_check_update", it).apply() 
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(120.dp))
            }
        }
    }
}

// --- 4. 辅助组件 ---

@Composable
fun PresetItemView(preset: PresetColor, isSelected: Boolean, textColor: Color, subTextColor: Color, onClick: () -> Unit) {
    val adaptiveColor = preset.color.adaptiveContentColor()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) preset.color.copy(alpha = 0.15f) else Color.Transparent)
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(preset.color).border(1.5.dp, Color.Gray.copy(alpha = 0.2f), CircleShape)) {
            if (isSelected) {
                Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp).align(Alignment.Center), tint = adaptiveColor)
            }
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(preset.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = textColor)
            Text(preset.desc, style = MaterialTheme.typography.labelSmall, color = subTextColor)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RGBSliderDialog(initialColor: Color, onDismiss: () -> Unit, onConfirm: (Color) -> Unit) {
    // 基础状态
    var r by remember { mutableFloatStateOf(initialColor.red * 255f) }
    var g by remember { mutableFloatStateOf(initialColor.green * 255f) }
    var b by remember { mutableFloatStateOf(initialColor.blue * 255f) }
    
    // HSL 状态用于彩虹进度条
    val hsvArray = remember { FloatArray(3) }
    android.graphics.Color.RGBToHSV(r.toInt(), g.toInt(), b.toInt(), hsvArray)
    var hue by remember { mutableFloatStateOf(hsvArray[0]) }
    var saturation by remember { mutableFloatStateOf(hsvArray[1]) }
    var brightness by remember { mutableFloatStateOf(hsvArray[2]) }

    val currentColor = Color(android.graphics.Color.HSVToColor((brightness * 255).toInt(), floatArrayOf(hue, saturation, brightness)))
    var hexText by remember { mutableStateOf(Integer.toHexString(currentColor.toArgb()).uppercase().takeLast(6)) }

    // 当 RGB 改变时同步 HSL 和 Hex
    LaunchedEffect(r, g, b) {
        val hsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(r.toInt(), g.toInt(), b.toInt(), hsv)
        hue = hsv[0]
        saturation = hsv[1]
        brightness = hsv[2]
        hexText = Integer.toHexString(currentColor.toArgb()).uppercase().takeLast(6)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("高级色彩自定义", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(20.dp))
                
                // 实时预览 & Hex 输入
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier.size(70.dp).clip(RoundedCornerShape(16.dp)).background(currentColor).border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    )
                    Spacer(Modifier.width(16.dp))
                    OutlinedTextField(
                        value = hexText,
                        onValueChange = { 
                            if (it.length <= 6) {
                                hexText = it
                                try {
                                    val parsedColor = android.graphics.Color.parseColor("#$it")
                                    r = android.graphics.Color.red(parsedColor).toFloat()
                                    g = android.graphics.Color.green(parsedColor).toFloat()
                                    b = android.graphics.Color.blue(parsedColor).toFloat()
                                } catch (e: Exception) {}
                            }
                        },
                        label = { Text("颜色代码") },
                        prefix = { Text("#") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                    )
                }
                
                Spacer(Modifier.height(24.dp))
                
                // 1. 彩虹色相条 (Hue)
                RGBSlider(
                    label = "色相", 
                    value = hue, 
                    valueRange = 0f..360f,
                    onValueChange = { 
                        hue = it 
                        val rgb = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, brightness))
                        r = android.graphics.Color.red(rgb).toFloat()
                        g = android.graphics.Color.green(rgb).toFloat()
                        b = android.graphics.Color.blue(rgb).toFloat()
                    },
                    trackBrush = Brush.horizontalGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red))
                )

                // 2. 亮度条 (Brightness/Value)
                RGBSlider(
                    label = "亮度", 
                    value = brightness * 100f, 
                    valueRange = 0f..100f,
                    onValueChange = { 
                        brightness = it / 100f
                        val rgb = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, brightness))
                        r = android.graphics.Color.red(rgb).toFloat()
                        g = android.graphics.Color.green(rgb).toFloat()
                        b = android.graphics.Color.blue(rgb).toFloat()
                    },
                    trackBrush = Brush.horizontalGradient(listOf(Color.Black, Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, 1f)))))
                )

                Spacer(Modifier.height(8.dp))
                PixelDivider()
                Spacer(Modifier.height(8.dp))

                // 3. RGB 独立通道
                RGBSlider(label = "R通道", value = r, onValueChange = { r = it }, trackBrush = Brush.horizontalGradient(listOf(Color.Black, Color.Red)))
                RGBSlider(label = "G通道", value = g, onValueChange = { g = it }, trackBrush = Brush.horizontalGradient(listOf(Color.Black, Color.Green)))
                RGBSlider(label = "B通道", value = b, onValueChange = { b = it }, trackBrush = Brush.horizontalGradient(listOf(Color.Black, Color.Blue)))
                
                Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Button(onClick = { onConfirm(currentColor) }, shape = RoundedCornerShape(12.dp)) { Text("确认应用") }
                }
            }
        }
    }
}

@Composable
fun RGBSlider(
    label: String, 
    value: Float, 
    valueRange: ClosedFloatingPointRange<Float> = 0f..255f,
    onValueChange: (Float) -> Unit, 
    trackBrush: Brush
) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value.toInt().toString(), style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
        }
        Box(modifier = Modifier.fillMaxWidth().height(32.dp), contentAlignment = Alignment.Center) {
            // 背景轨道
            Box(modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(trackBrush))
            // Slider
            Slider(
                value = value, 
                onValueChange = onValueChange, 
                valueRange = valueRange,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
fun SettingsSection(
    title: String, 
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    isExpanded: Boolean, 
    onToggle: () -> Unit,
    animationSpecSize: FiniteAnimationSpec<IntSize> = spring(stiffness = Spring.StiffnessMediumLow),
    animationSpecFloat: FiniteAnimationSpec<Float> = spring(stiffness = Spring.StiffnessMediumLow),
    outlineColor: Color = MaterialTheme.colorScheme.outline,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable ColumnScope.() -> Unit
) {
    // 洛琪希逻辑：根据亮暗色模式动态调整容器底色，增强层次感
    val containerColor = if (isSystemInDarkTheme()) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    } else {
        primaryColor.copy(alpha = 0.03f)
    }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(), 
        shape = RoundedCornerShape(24.dp), 
        // 洛琪希优化：此处 outlineColor 已由调用处传入 primary 色的 alpha 版
        border = BorderStroke(1.2.dp, outlineColor),
        colors = CardDefaults.outlinedCardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth().clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onToggle() }, verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = primaryColor, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Icon(if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, tint = primaryColor)
            }
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpecSize) + fadeIn(animationSpecFloat),
                exit = shrinkVertically(animationSpecSize) + fadeOut(animationSpecFloat)
            ) {
                Column { Spacer(Modifier.height(16.dp)); content() }
            }
        }
    }
}

@Composable
fun SettingsToggleItem(
    title: String, 
    subtitle: String, 
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    checked: Boolean, 
    enabled: Boolean = true, 
    textColor: Color = Color.Unspecified,
    subTextColor: Color = Color.Unspecified,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.5f).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        // 洛琪希优化：图标颜色强制跟随 primaryColor 以达成狠狠适配
        Icon(icon, null, modifier = Modifier.size(24.dp), tint = primaryColor)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = textColor)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = subTextColor)
        }
        Switch(
            checked = checked, 
            onCheckedChange = onCheckedChange, 
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = primaryColor,
                uncheckedThumbColor = Color.Gray.copy(alpha = 0.5f),
                uncheckedTrackColor = Color.Transparent
            )
        )
    }
}
