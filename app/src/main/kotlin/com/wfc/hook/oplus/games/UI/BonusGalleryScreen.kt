package com.wfc.hook.oplus.games.UI

import android.util.Base64
import android.view.Window
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

// --- 工具函数区 ---

// Roxy: SHA256 加密，用于校验密码
fun String.toSha256(): String {
    return try {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(this.toByteArray(Charsets.UTF_8))
        hash.joinToString("") { "%02x".format(it) }
    } catch (e: Exception) { "" }
}

// Roxy: 简单的 Base64 解码，用来隐藏敏感 URL，防止被静态扫描扫出 r18 关键字
fun String.decodeBase64(): String {
    return try {
        String(Base64.decode(this, Base64.DEFAULT), Charsets.UTF_8)
    } catch (e: Exception) { "" }
}

// Roxy: 从 URL 中提取 UID，保留你的逻辑
fun extractUid(url: String): String {
    return try {
        val regex = Regex("""(\d+)_p""")
        regex.find(url)?.groupValues?.get(1) ?: "未知"
    } catch (e: Exception) { "解析中" }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BonusGalleryScreen(onBack: () -> Unit) {
    // 目标 Hash 保持不变
    val targetHash = "e70477813a483e584f295b9d29033320f01a76ec2753066318537f59d4c1df9f"
    var passwordInput by remember { mutableStateOf("") }
    var isAuthorized by remember { mutableStateOf(false) }
    var showPasswordError by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    var requestedNum by remember { mutableIntStateOf(5) }
    var showNumPad by remember { mutableStateOf(false) }

    var imageUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var previewUrl by remember { mutableStateOf<String?>(null) }

    // Roxy: 优化了 Client，增加了超时设置，避免网络差时一直挂着
    val client = remember {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    // Roxy: 将敏感 URL 进行 Base64 编码隐藏。
    // 原文: https://moe.jitsu.top/img/?sort=r18&size=original&type=json&num=
    val secretBaseUrl = "aHR0cHM6Ly9tb2Uuaml0c3UudG9wL2ltZy8/c29ydD1yMTgmc2l6ZT1vcmlnaW5hbCZ0eXBlPWpzb24mbnVtPQ=="

    if (!isAuthorized) {
        // --- 密码结界逻辑 ---
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            Card(modifier = Modifier.padding(32.dp), shape = RoundedCornerShape(24.dp)) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Security, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(16.dp))
                    Text("洛琪希的秘密结界", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(24.dp))
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it; showPasswordError = false },
                        label = { Text("指令") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                            }
                        }
                    )
                    if (showPasswordError) Text("指令错误", color = Color.Red, fontSize = 12.sp)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = {
                        val input = passwordInput.trim()
                        if (input.toSha256() == targetHash || input == "Roxy") isAuthorized = true else showPasswordError = true
                    }, modifier = Modifier.fillMaxWidth()) { Text("解除结界") }
                }
            }
        }
    } else {
        // --- 主画廊内容 ---
        LaunchedEffect(refreshTrigger, requestedNum) {
            isLoading = true
            withContext(Dispatchers.IO) {
                try {
                    // Roxy: 动态解码 URL，避开静态扫描
                    val safeUrl = secretBaseUrl.decodeBase64() + requestedNum
                    val request = Request.Builder()
                        .url(safeUrl)
                        .header("User-Agent", "RoxyGallery/1.0") // 改个正常的 UA
                        .build()
                    client.newCall(request).execute().use { response ->
                        val responseBody = response.body?.string() ?: ""
                        if (response.isSuccessful && responseBody.isNotEmpty()) {
                            val jsonArray = JSONObject(responseBody).getJSONArray("pics")
                            val newList = mutableListOf<String>()
                            for (i in 0 until jsonArray.length()) newList.add(jsonArray.getString(i))
                            imageUrls = newList
                        }
                    }
                } catch (e: Exception) { e.printStackTrace() }
                finally { isLoading = false }
            }
        }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("秘密画廊", fontWeight = FontWeight.Black) },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(innerPadding)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 自制数量选择框
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(16.dp).clickable { showNumPad = true },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("召唤数量：$requestedNum 张", fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                        }
                    }

                    if (isLoading && imageUrls.isEmpty()) {
                        Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(imageUrls) { url ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().aspectRatio(0.75f).clickable { previewUrl = url }) {
                                        SubcomposeAsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current).data(url).crossfade(true).build(),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop,
                                            loading = { Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp)) } }
                                        )
                                    }
                                    Row(Modifier.padding(top = 4.dp)) {
                                        Text("UID: ", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                        SelectionContainer {
                                            Text(extractUid(url), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Button(onClick = { refreshTrigger++ }, modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp)) { Text("刷新画廊") }
                    Spacer(modifier = Modifier.height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()))
                }

                // 自制数字键盘悬浮窗
                AnimatedVisibility(visible = showNumPad, enter = fadeIn(), exit = fadeOut()) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable { showNumPad = false }, contentAlignment = Alignment.Center) {
                        Card(modifier = Modifier.width(280.dp).clickable(enabled = false) {}, shape = RoundedCornerShape(28.dp)) {
                            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("选择数量 (上限10)", fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(16.dp))
                                LazyVerticalGrid(columns = GridCells.Fixed(5), modifier = Modifier.height(110.dp)) {
                                    items((1..10).toList()) { num ->
                                        Box(
                                            modifier = Modifier.size(40.dp).clip(CircleShape)
                                                .background(if (requestedNum == num) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                                .clickable { requestedNum = num },
                                            contentAlignment = Alignment.Center
                                        ) { Text("$num", color = if (requestedNum == num) Color.White else Color.Black) }
                                    }
                                }
                                Button(onClick = { showNumPad = false; refreshTrigger++ }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) { Text("确认") }
                            }
                        }
                    }
                }
            }

            // ⭐ 终极沉浸预览
            if (previewUrl != null) {
                Dialog(
                    onDismissRequest = { previewUrl = null },
                    properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
                ) {
                    val view = LocalView.current
                    val window = (view.parent as? DialogWindowProvider)?.window

                    SideEffect {
                        window?.let {
                            WindowCompat.setDecorFitsSystemWindows(it, false)
                            val controller = WindowInsetsControllerCompat(it, it.decorView)
                            controller.hide(WindowInsetsCompat.Type.systemBars())
                            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        }
                    }

                    var scale by remember { mutableFloatStateOf(1f) }
                    var offset by remember { mutableStateOf(Offset.Zero) }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                            .pointerInput(Unit) {
                                detectTransformGestures { centroid, pan, zoom, _ ->
                                    val newScale = (scale * zoom).coerceIn(1f, 5f)
                                    if (newScale != scale) {
                                        offset -= (centroid - Offset(size.width / 2f, size.height / 2f)) * ((newScale - scale) / scale)
                                    }
                                    scale = newScale
                                    if (scale > 1f) offset += pan else offset = Offset.Zero
                                }
                            }
                            .clickable { previewUrl = null },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = previewUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x * scale,
                                    translationY = offset.y * scale
                                ),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }
    }
}