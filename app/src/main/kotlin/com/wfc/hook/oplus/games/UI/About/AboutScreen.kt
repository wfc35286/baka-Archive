package com.wfc.hook.oplus.games.UI.About

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File 
import java.net.URL
import com.wfc.hook.oplus.games.RootUtils
//import com.wfc.hook.oplus.games.UI.UpdateScreen

@Composable
fun PixelDividerForAbout() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 4.dp)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AboutScreen(
    onNavigateToBonus: () -> Unit, 
    onNavigateToSettings: () -> Unit,
    onNavigateToUpdate: () -> Unit,
    onBackToHome: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("ui_settings", Context.MODE_PRIVATE) }
    
    var clickCount by remember { mutableIntStateOf(0) }
    var lastClickTime by remember { mutableLongStateOf(0L) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    
    var isDisclaimerExpanded by remember { 
        mutableStateOf(prefs.getBoolean("disclaimer_expanded", true)) 
    }
    
    var disclaimerPositionY by remember { mutableFloatStateOf(0f) }
    var disclaimerText by remember { mutableStateOf("加载中...") }
    val defaultDisclaimer = "本软件为开源辅助工具，旨在提升用户体验。使用本软件产生的任何系统不稳定性或因操作不当导致的风险，开发者不承担任何形式的连带责任。请在合法及系统允许的范围内使用。"

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val text = URL("https://raw.githubusercontent.com/wfc35286/baka-update/main/DISCLAIMER.txt")
                    .readText()
                withContext(Dispatchers.Main) {
                    disclaimerText = text.ifBlank { defaultDisclaimer }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    disclaimerText = defaultDisclaimer
                }
            }
        }
    }

    LaunchedEffect(isDisclaimerExpanded) {
        if (isDisclaimerExpanded) {
            coroutineScope.launch {
                scrollState.animateScrollTo(
                    value = disclaimerPositionY.toInt(),
                    animationSpec = spring(stiffness = Spring.StiffnessLow)
                )
            }
        }
    }
    
    val appVersionName = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName
        } catch (e: Exception) {
            "Unknown"
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "flow_light")
    val flowColors = listOf(Color(0xFF6750A4), Color(0xFF984061), Color(0xFF2196F3))
    val colorA by infiniteTransition.animateColor(initialValue = flowColors[0], targetValue = flowColors[1], animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Reverse), label = "A")
    val colorB by infiniteTransition.animateColor(initialValue = flowColors[1], targetValue = flowColors[2], animationSpec = infiniteRepeatable(tween(3500, easing = LinearEasing), RepeatMode.Reverse), label = "B")
    val colorC by infiniteTransition.animateColor(initialValue = flowColors[2], targetValue = flowColors[0], animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Reverse), label = "C")
    val primaryBrush = Brush.linearGradient(colors = listOf(colorA, colorB, colorC))
    val openUrl = { url: String -> try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (e: Exception) {} }

    if (showUpdateDialog) { UpdateDialog(onDismiss = { showUpdateDialog = false }) }
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认重置？", fontWeight = FontWeight.Bold) },
            text = { Text("这将移除 /data/local/tmp/rkx 下的所有配置文件，操作不可撤销。") },
            confirmButton = { 
                Button(
                    onClick = { 
                        showDeleteDialog = false 
                        try {
                            val isSuccess = RootUtils.resetAllConfigs()
                            if (isSuccess) {
                                Toast.makeText(context, "配置已彻底粉碎 🪄", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "粉碎失败：请确认已授予 Root 权限", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "操作异常: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }, 
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("确认重置") } 
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("取消") } }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(Modifier.height(4.dp))

        // 1. 顶部流光卡片 (使用 Surface 组合点击)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null, 
                    onClick = {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastClickTime < 500) {
                            clickCount++
                        } else {
                            clickCount = 1
                        }
                        lastClickTime = currentTime
                        
                        if (clickCount >= 8) { 
                            clickCount = 0
                            onNavigateToBonus() 
                        }
                    }
                ),
            shape = RoundedCornerShape(32.dp),
            tonalElevation = 8.dp,
            shadowElevation = 6.dp,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
        ) {
            Box(modifier = Modifier.background(primaryBrush).padding(vertical = 36.dp, horizontal = 24.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        shape = RoundedCornerShape(24.dp), 
                        color = Color.White.copy(alpha = 0.2f), 
                        modifier = Modifier.size(80.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                    ) {
                        Icon(Icons.Filled.AutoAwesome, null, tint = Color.White, modifier = Modifier.padding(20.dp).size(40.dp))
                    }
                    Spacer(Modifier.height(20.dp))
                    Text("游戏助手增强", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = Color.White)
                    Spacer(Modifier.height(4.dp))
                    Surface(color = Color.Black.copy(alpha = 0.2f), shape = CircleShape) {
                        val displayVersion = "$appVersionName Ultimate"
                        Text("Version $displayVersion", modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp), color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. 功能交互区
        Surface(
            onClick = { showUpdateDialog = true },
            shape = RoundedCornerShape(24.dp), 
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Icon(Icons.Filled.SystemUpdate, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text("检测并同步新魔法", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }

        // 3. 安全声明
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader("安全与隐私", Icons.Filled.VerifiedUser)
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SecurityNoteItem("不收集任何用户隐私数据")
                    SecurityNoteItem("全程无网络请求，离线可用")
                    SecurityNoteItem("Root 权限仅用于同步系统配置")
                }
            }
        }

        // 4. 开源致谢
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader("开源致谢", Icons.Filled.Source)
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))) {
                Column {
                    OpenSourceItem("Jetpack Compose", "现代声明式 UI 框架", "https://developer.android.com/compose", openUrl)
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    OpenSourceItem("LSPosed", "系统 Hook 核心支持", "https://github.com/LSPosed/LSPosed", openUrl)
                }
            }
        }

        // 5. 免责声明 (修复：使用 Surface 的 onClick 参数)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    disclaimerPositionY = coordinates.positionInParent().y
                },
            // ⭐ 核心修复：移除 Modifier.clickable，改用 onClick 参数
            onClick = { 
                isDisclaimerExpanded = !isDisclaimerExpanded
                prefs.edit().putBoolean("disclaimer_expanded", isDisclaimerExpanded).apply()
            },
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.animateContentSize()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Gavel, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text(text = "免责声明", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                    Icon(
                        imageVector = if (isDisclaimerExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }

                if (isDisclaimerExpanded) {
                    PixelDividerForAbout()
                    Text(
                        text = disclaimerText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Justify
                    )
                }
            }
        }

        // 6. 实验室选项
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader("实验室选项", Icons.Filled.BugReport)
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    onClick = { 
                        try {
                            var isCleaned = false
                            val apks = listOf("baka_update.apk", "baka_update.tmp")
                            apks.forEach { 
                                val file = File(context.cacheDir, it)
                                if (file.exists() && file.delete()) isCleaned = true
                            }
                            val cacheFolders = listOf("image_cache", "coil_cache", "p_cache")
                            cacheFolders.forEach { folderName ->
                                val dir = File(context.cacheDir, folderName)
                                if (dir.exists() && dir.deleteRecursively()) isCleaned = true
                            }
                            Toast.makeText(context, if(isCleaned) "已抹除所有残留魔法 ✨" else "环境非常整洁 ~", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "清理失败: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp), 
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.CleaningServices, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("清理缓存", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }

                Surface(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp), 
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.DeleteSweep, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("重置配置", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Created by Baka-Admin 2026 🥵", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
        Icon(icon, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(text = title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun SecurityNoteItem(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.CheckCircle, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
        Spacer(Modifier.width(12.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun OpenSourceItem(name: String, desc: String, url: String, onItemClick: (String) -> Unit) {
    Surface(onClick = { onItemClick(url) }, color = Color.Transparent) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            // 使用 AutoMirrored 修复图标镜像警告
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }
    }
}