package com.wfc.hook.oplus.games.UI

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.documentfile.provider.DocumentFile
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL

// --- 数据模型 ---
data class ShopItem(
    val name: String,
    val version: String,
    val type: String,
    val downloadUrl: String,
    val description: String,
    val iconUrl: String
)

class DownloadTaskState(
    val item: ShopItem,
    var progress: MutableState<Float> = mutableStateOf(0f),
    var isDownloading: MutableState<Boolean> = mutableStateOf(false),
    var isPaused: MutableState<Boolean> = mutableStateOf(false),
    var speedText: MutableState<String> = mutableStateOf("待命"),
    var sizeText: MutableState<String> = mutableStateOf("0/0 MB"),
    var isFinished: MutableState<Boolean> = mutableStateOf(false),
    var job: Job? = null,
    var currentBytes: Long = 0L 
)

// --- 路径与缓存管理 ---
object DownloadSettings {
    private const val PREF_NAME = "roxy_shop_ultimate_prefs"
    private const val KEY_URI = "download_folder_uri"

    fun saveUri(context: Context, uri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            uri, 
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_URI, uri.toString()).apply()
    }

    fun getUri(context: Context): Uri? {
        val uriString = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString(KEY_URI, null)
        return uriString?.let { Uri.parse(it) }
    }

    fun checkExists(context: Context, item: ShopItem): Boolean {
        val uri = getUri(context) ?: return false
        return try {
            val root = DocumentFile.fromTreeUri(context, uri) ?: return false
            root.findFile("${item.name}_v${item.version}.apk")?.exists() == true
        } catch (e: Exception) { false }
    }
}

// --- 下载引擎 ---
object ShopDownloadManager {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun startOrResume(context: Context, state: DownloadTaskState) {
        if (state.isDownloading.value) return
        val targetUri = DownloadSettings.getUri(context) ?: run {
            Toast.makeText(context, "だんな様，请先选择下载目录！", Toast.LENGTH_SHORT).show()
            return
        }

        state.isDownloading.value = true
        state.isPaused.value = false
        
        state.job = scope.launch {
            try {
                val fileName = "${state.item.name}_v${state.item.version}.apk"
                val rootDoc = DocumentFile.fromTreeUri(context, targetUri) ?: throw Exception()
                var targetFile = rootDoc.findFile(fileName)

                if (targetFile == null || !targetFile.exists()) {
                    state.currentBytes = 0L
                    targetFile = rootDoc.createFile("application/vnd.android.package-archive", fileName)
                } else {
                    state.currentBytes = targetFile.length()
                }

                val conn = (URL(state.item.downloadUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10000
                    if (state.currentBytes > 0) {
                        setRequestProperty("Range", "bytes=${state.currentBytes}-")
                    }
                }

                val responseCode = conn.responseCode
                val totalSize = if (responseCode == 206) {
                    state.currentBytes + conn.contentLength.toLong()
                } else {
                    state.currentBytes = 0L
                    conn.contentLength.toLong()
                }

                var lastTime = System.currentTimeMillis()
                var lastBytes = state.currentBytes
                var downloaded = state.currentBytes

                context.contentResolver.openOutputStream(targetFile!!.uri, "wa")!!.use { output ->
                    conn.inputStream.use { input ->
                        val buffer = ByteArray(16384)
                        var read: Int
                        while (isActive) {
                            read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            state.currentBytes = downloaded

                            val now = System.currentTimeMillis()
                            if (now - lastTime >= 950) {
                                val speed = (downloaded - lastBytes) * 1000 / (now - lastTime)
                                withContext(Dispatchers.Main) {
                                    state.progress.value = downloaded.toFloat() / totalSize
                                    state.speedText.value = if (speed < 1048576) "${speed / 1024} KB/s" else "%.2f MB/s".format(speed / 1048576f)
                                    state.sizeText.value = "%.1f/%.1f MB".format(downloaded / 1048576f, totalSize / 1048576f)
                                }
                                lastTime = now; lastBytes = downloaded
                            }
                        }
                    }
                }
                
                if (isActive) withContext(Dispatchers.Main) {
                    state.isDownloading.value = false; state.isFinished.value = true; state.progress.value = 1f
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { 
                    state.isDownloading.value = false
                    state.speedText.value = "已暂停/失败" 
                }
            }
        }
    }

    fun pause(state: DownloadTaskState) {
        state.job?.cancel()
        state.isDownloading.value = false
        state.isPaused.value = true
        state.speedText.value = "已暂停"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var allItems by remember { mutableStateOf<List<ShopItem>>(emptyList()) }
    var categories by remember { mutableStateOf(listOf("全部")) }
    var selectedCategory by remember { mutableStateOf("全部") }
    var isLoading by remember { mutableStateOf(true) }
    var showTasks by remember { mutableStateOf(false) }
    val taskMap = remember { mutableStateMapOf<String, DownloadTaskState>() }

    val dirLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let { DownloadSettings.saveUri(context, it) }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val rawText = URL("https://raw.githubusercontent.com/wfc35286/baka-update/main/shop_list.ini").readText()
                val parsed = rawText.lines()
                    .filter { it.startsWith("item=") }
                    .mapNotNull { line ->
                        val p = line.removePrefix("item=").split("|")
                        if (p.size >= 4) {
                            ShopItem(p[0].trim(), p[1].trim(), p[2].trim(), p[3].trim(), p.getOrNull(4)?.trim() ?: "", p.getOrNull(5)?.trim() ?: "")
                        } else null
                    }
                    .distinctBy { "${it.name}_${it.type}_${it.version}" }

                withContext(Dispatchers.Main) {
                    allItems = parsed
                    categories = listOf("全部") + parsed.map { it.type }.distinct()
                    isLoading = false
                    allItems.forEach { 
                        val uniqueKey = "${it.downloadUrl}_${it.name}_${it.type}"
                        if (!taskMap.containsKey(uniqueKey)) {
                            val state = DownloadTaskState(it)
                            if (DownloadSettings.checkExists(context, it)) state.isFinished.value = true
                            taskMap[uniqueKey] = state
                        }
                    }
                }
            } catch (e: Exception) { withContext(Dispatchers.Main) { isLoading = false } }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column(Modifier.background(MaterialTheme.colorScheme.surface).statusBarsPadding()) {
                // --- 顶部文字标题 (对齐 HomeScreen 风格) ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 32.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(6.dp)) {}
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "资源中心", 
                                style = MaterialTheme.typography.labelLarge, 
                                color = MaterialTheme.colorScheme.primary, 
                                letterSpacing = 2.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "魔法资源商店", 
                            style = MaterialTheme.typography.headlineLarge, 
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Row {
                        IconButton(onClick = { dirLauncher.launch(null) }) { Icon(Icons.Rounded.FolderSpecial, null, tint = MaterialTheme.colorScheme.primary) }
                        IconButton(onClick = { showTasks = true }) { Icon(Icons.Rounded.History, null) }
                    }
                }

                if (categories.size > 1) {
                    ScrollableTabRow(
                        selectedTabIndex = categories.indexOf(selectedCategory).coerceAtLeast(0),
                        edgePadding = 24.dp,
                        containerColor = Color.Transparent,
                        divider = {},
                        indicator = { tabPositions ->
                            val currentTabIndex = categories.indexOf(selectedCategory).coerceAtLeast(0)
                            if (currentTabIndex < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[currentTabIndex]),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    ) {
                        categories.forEach { category ->
                            Tab(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category },
                                text = { Text(category, fontSize = 14.sp, fontWeight = if(selectedCategory == category) FontWeight.Bold else FontWeight.Normal) }
                            )
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        val filteredItems = if (selectedCategory == "全部") allItems else allItems.filter { it.type == selectedCategory }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }
            if (isLoading) {
                item { Box(Modifier.fillMaxWidth().height(200.dp), Alignment.Center) { CircularProgressIndicator() } }
            } else {
                itemsIndexed(
                    items = filteredItems, 
                    key = { _, item -> "${item.name}_${item.type}_${item.version}" }
                ) { _, item ->
                    val uniqueKey = "${item.downloadUrl}_${item.name}_${item.type}"
                    val state = taskMap[uniqueKey] ?: DownloadTaskState(item)
                    AdaptiveShopCard(item, state)
                }
            }
            item { Spacer(Modifier.height(padding.calculateBottomPadding() + 20.dp)) }
        }
    }
    if (showTasks) TaskDialog(taskMap.values.toList()) { showTasks = false }
}

@Composable
fun AdaptiveShopCard(item: ShopItem, state: DownloadTaskState) {
    val context = LocalContext.current
    var isIconLoaded by remember { mutableStateOf(false) }
    var isTimeout by remember { mutableStateOf(false) }
    
    // 10秒超时逻辑
    LaunchedEffect(item.iconUrl) {
        if (item.iconUrl.isNotBlank()) {
            delay(10000)
            if (!isIconLoaded) isTimeout = true
        } else {
            isTimeout = true
        }
    }

    val useIconUI = !isTimeout && item.iconUrl.isNotBlank()
    
    val containerColor = if (!isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                         else MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
    
    val borderColor = if (!isSystemInDarkTheme()) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                      else Color.White.copy(alpha = 0.1f)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (useIconUI) {
                    AsyncImage(
                        model = item.iconUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .border(0.5.dp, borderColor, RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                        onState = { 
                            if (it is AsyncImagePainter.State.Success) isIconLoaded = true
                            if (it is AsyncImagePainter.State.Error) isTimeout = true
                        }
                    )
                    Spacer(Modifier.width(12.dp))
                }
                
                Column(Modifier.weight(1f)) {
                    Text(item.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${item.type} · v${item.version}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.isFinished.value && !state.isDownloading.value) {
                        IconButton(onClick = {
                            DownloadSettings.getUri(context)?.let { uri ->
                                try {
                                    DocumentFile.fromTreeUri(context, uri)?.findFile("${item.name}_v${item.version}.apk")?.delete()
                                    state.isFinished.value = false; state.progress.value = 0f; state.currentBytes = 0L
                                } catch (e: Exception) { }
                            }
                        }) { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error.copy(0.7f)) }
                    }

                    IconButton(
                        onClick = { 
                            when {
                                state.isFinished.value -> handleAction(context, item)
                                state.isDownloading.value -> ShopDownloadManager.pause(state)
                                else -> ShopDownloadManager.startOrResume(context, state)
                            }
                        }
                    ) {
                        when {
                            state.isDownloading.value -> Icon(Icons.Rounded.PauseCircleFilled, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                            state.isPaused.value && state.progress.value > 0 -> Icon(Icons.Rounded.PlayCircleFilled, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                            state.isFinished.value -> {
                                if (item.type.contains("模块")) Icon(Icons.Rounded.Inventory, null, tint = Color(0xFFE91E63))
                                else Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF4CAF50))
                            }
                            else -> Icon(Icons.Rounded.DownloadForOffline, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            
            if (item.description.isNotBlank()) {
                Text(item.description, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp, start = 2.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }

            AnimatedVisibility(state.isDownloading.value || (state.isPaused.value && state.progress.value > 0)) {
                Column(Modifier.padding(top = 12.dp)) {
                    LinearProgressIndicator(progress = { state.progress.value }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape))
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), Arrangement.SpaceBetween) {
                        Text(state.speedText.value, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text(state.sizeText.value, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
fun TaskDialog(tasks: List<DownloadTaskState>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(Modifier.fillMaxWidth().fillMaxHeight(0.7f), shape = RoundedCornerShape(28.dp), tonalElevation = 6.dp) {
            Column(Modifier.padding(24.dp)) {
                Text("传输中心", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(16.dp))
                val active = tasks.filter { it.isDownloading.value || it.progress.value > 0 }
                if (active.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) { Text("干净溜溜，没有任何任务", color = MaterialTheme.colorScheme.outline) }
                } else {
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        itemsIndexed(active, key = { index, _ -> "task_$index" }) { _, task ->
                            Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.secondaryContainer.copy(0.4f), RoundedCornerShape(16.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(task.item.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    LinearProgressIndicator(progress = { task.progress.value }, Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(CircleShape))
                                }
                                IconButton(onClick = {
                                    task.job?.cancel(); task.isDownloading.value = false; task.progress.value = 0f; task.isFinished.value = false; task.currentBytes = 0L
                                    DownloadSettings.getUri(context)?.let { uri -> DocumentFile.fromTreeUri(context, uri)?.findFile("${task.item.name}_v${task.item.version}.apk")?.delete() }
                                }) { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }
                }
                Button(onClick = onDismiss, Modifier.align(Alignment.End).padding(top = 16.dp)) { Text("收起") }
            }
        }
    }
}

fun handleAction(context: Context, item: ShopItem) {
    val folderUri = DownloadSettings.getUri(context) ?: return
    try {
        val root = DocumentFile.fromTreeUri(context, folderUri) ?: return
        val targetFile = root.findFile("${item.name}_v${item.version}.apk") ?: return
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(targetFile.uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) { 
        Toast.makeText(context, "无法启动安装程序", Toast.LENGTH_SHORT).show() 
    }
}
