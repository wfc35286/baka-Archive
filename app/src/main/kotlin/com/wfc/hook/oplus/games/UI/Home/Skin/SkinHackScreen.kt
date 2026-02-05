package com.wfc.hook.oplus.games.UI.Home.Skin

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale 
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.wfc.hook.oplus.games.RootUtils
import com.wfc.hook.oplus.games.R 

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkinHackScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var activeSkin by remember { mutableStateOf("nt") }
    val scrollState = rememberScrollState()

    val skinList = listOf(
        Triple("genshin-keqing", "原神 - 刻晴", "genshin_keqing"),
        Triple("genshin-hutao", "原神 - 胡桃", "genshin_hutao"),
        Triple("genshin-shenlilinghua", "原神 - 神里绫华", "genshin_shenlilinghua"),
        Triple("genshin-xiangling", "原神 - 香菱", "genshin_xiangling"),
        Triple("naruto", "火影忍者", "naruto")
    )
    
    LaunchedEffect(Unit) {
        val current = withContext(Dispatchers.IO) { RootUtils.readSkinConfig(context) }
        activeSkin = if (current.isBlank()) "nt" else current
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text("皮肤中心", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBackIosNew, contentDescription = "返回", modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
                // ステータスバーとカメラ穴を回避
                windowInsets = WindowInsets.statusBars.union(WindowInsets.displayCutout)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(top = 12.dp)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. 顶部艺术化卡片 (AutoCombe と色を統一: Primary Base)
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Filled.AutoAwesome, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(10.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("视觉重塑", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("点击下方列表切换助手皮肤", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            
            Text("角色列表", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp))

            // 2. 皮肤列表区域
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                skinList.forEach { (skinKey, skinName, resName) ->
                    val isSelected = activeSkin == skinKey
                    val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)
                    
                    // ✨ 修复动画: 使用 animateFloat 替代 AnimatedVisibility
                    // 这样可以避免布局计算导致的“扇形”裁剪问题
                    val scaleAnim by animateFloatAsState(
                        targetValue = if (isSelected) 1f else 0f,
                        animationSpec = tween(300),
                        label = "scale"
                    )

                    Surface(
                        onClick = { activeSkin = if (isSelected) "nt" else skinKey },
                        shape = RoundedCornerShape(24.dp), 
                        // 统一颜色逻辑: 选中用 PrimaryContainer，未选中用 SurfaceVariant
                        color = if (isSelected) 
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
                        else 
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, 
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                Image(
                                    painter = painterResource(id = if (resId != 0) resId else android.R.drawable.ic_menu_gallery),
                                    contentDescription = skinName,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.surface),
                                    contentScale = ContentScale.Crop
                                )
                                
                                // ✨ 动画修复点: 只有当 scale > 0 时才显示，避免占位
                                if (scaleAnim > 0f) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .offset(x = 6.dp, y = 6.dp)
                                            .scale(scaleAnim) // 使用简单的 Scale 变换
                                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                    ) {
                                        Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.padding(3.dp))
                                    }
                                }
                            }

                            Spacer(Modifier.width(16.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(skinName, style = MaterialTheme.typography.bodyLarge, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                                Text(
                                    if (isSelected) "当前应用" else "点击应用", 
                                    style = MaterialTheme.typography.bodySmall, 
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Switch(
                                checked = isSelected,
                                onCheckedChange = { activeSkin = if (it) skinKey else "nt" },
                                modifier = Modifier.scale(0.8f),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    uncheckedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }
            }

            // 3. 操作组 (按钮样式统一为 Primary)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) { RootUtils.saveSkinConfig(context, activeSkin) }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Filled.Save, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("保存皮肤设定", fontWeight = FontWeight.Bold)
                }
                
                // 统一样式: Primary 边框和文字
                FilledTonalButton(
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) { RootUtils.restartGameHelper() }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                ) {
                    Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("重启助手生效", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                }
            }
            
            // 4. 说明卡片
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Filled.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("若显示异常请恢复默认(nt)。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
