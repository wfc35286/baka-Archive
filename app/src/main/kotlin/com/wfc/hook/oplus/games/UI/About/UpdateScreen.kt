package com.wfc.hook.oplus.games.UI.About

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

// ==========================================
// 逻辑数据层 (洛琪希严正声明：绝对不缺斤少两)
// ==========================================

data class UpdateData(
    val version: String,
    val title: String,
    val changelog: String,
    val downloadUrl: String,
    val remoteSha256: String,
    val isNewVersion: Boolean,
    val totalSize: Long = 0L
)

enum class DownloadStatus {
    IDLE, DOWNLOADING, PAUSED, COMPLETED, FAILED
}

object BakaDownloadManager {
    var status by mutableStateOf(DownloadStatus.IDLE)
    var progress by mutableStateOf(0f)
    var speedText by mutableStateOf("0 KB/s")
    var sizeText by mutableStateOf("0 MB / 0 MB")
    private var downloadJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun start(context: Context, info: UpdateData, tmpFile: File, apkFile: File) {
        if (status == DownloadStatus.DOWNLOADING) return
        status = DownloadStatus.DOWNLOADING
        downloadJob = scope.launch {
            try {
                val startBytes = if (tmpFile.exists()) tmpFile.length() else 0L
                val url = URL(info.downloadUrl)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    if (startBytes > 0) setRequestProperty("Range", "bytes=$startBytes-")
                    connectTimeout = 10000
                }
                
                val totalLength = if (startBytes > 0 && (conn.responseCode == 206)) {
                    conn.contentLength.toLong() + startBytes
                } else {
                    conn.contentLength.toLong()
                }

                var lastTime = System.currentTimeMillis()
                var lastBytes = startBytes

                conn.inputStream.use { input ->
                    RandomAccessFile(tmpFile, "rw").use { raf ->
                        if (startBytes > 0 && conn.responseCode == 206) raf.seek(startBytes) else raf.setLength(0)
                        val buffer = ByteArray(8192)
                        var read: Int
                        
                        while (isActive) {
                            read = input.read(buffer)
                            if (read == -1) break
                            
                            raf.write(buffer, 0, read)
                            val current = raf.length()
                            progress = current.toFloat() / totalLength
                            
                            val now = System.currentTimeMillis()
                            if (now - lastTime >= 800) { 
                                val timeDiff = now - lastTime
                                val byteDiff = current - lastBytes
                                val speed = (byteDiff * 1000 / timeDiff) / 1024
                                speedText = "$speed KB/s"
                                sizeText = "%.1f MB / %.1f MB".format(current/1024f/1024f, totalLength/1024f/1024f)
                                lastTime = now
                                lastBytes = current
                                updateLiveNotification(context, (progress * 100).toInt())
                            }
                        }
                    }
                }
                
                if (isActive) {
                    if (info.remoteSha256.isEmpty() || getFileSha256(tmpFile).equals(info.remoteSha256, true)) {
                        if (apkFile.exists()) apkFile.delete()
                        tmpFile.renameTo(apkFile)
                        status = DownloadStatus.COMPLETED
                        updateLiveNotification(context, 100, isFinished = true)
                    } else {
                        tmpFile.delete()
                        status = DownloadStatus.FAILED
                        updateLiveNotification(context, 0, isFailed = true)
                    }
                }
            } catch (e: Exception) {
                status = DownloadStatus.FAILED
                updateLiveNotification(context, 0, isFailed = true)
            }
        }
    }

    fun stop() {
        downloadJob?.cancel()
        status = DownloadStatus.PAUSED
        speedText = "已暂停"
    }
}

fun isNewerVersion(local: String, remote: String): Boolean {
    val lParts = local.filter { it.isDigit() || it == '.' }.split(".").map { it.toIntOrNull() ?: 0 }
    val rParts = remote.filter { it.isDigit() || it == '.' }.split(".").map { it.toIntOrNull() ?: 0 }
    for (i in 0 until maxOf(lParts.size, rParts.size)) {
        val l = lParts.getOrNull(i) ?: 0
        val r = rParts.getOrNull(i) ?: 0
        if (r > l) return true
        if (l > r) return false
    }
    return false
}

fun getFileSha256(file: File): String {
    if (!file.exists()) return ""
    return try {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var readLen: Int
            while (input.read(buffer).also { readLen = it } != -1) {
                md.update(buffer, 0, readLen)
            }
        }
        md.digest().joinToString("") { "%02x".format(it) }
    } catch (e: Exception) { "" }
}

fun installApk(context: Context, file: File) {
    try {
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "安装失败: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

private fun updateLiveNotification(context: Context, progress: Int, isFinished: Boolean = false, isFailed: Boolean = false) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelId = "UPDATE_CHANNEL"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager.getNotificationChannel(channelId) == null) {
        manager.createNotificationChannel(NotificationChannel(channelId, "应用更新", NotificationManager.IMPORTANCE_LOW))
    }
    val notificationId = 1001
    if (isFinished || isFailed) {
        manager.cancel(notificationId)
        if (isFinished) {
            val success = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("同步完成").setContentText("点击安装新魔法").setAutoCancel(true).build()
            manager.notify(1002, success)
        }
        return
    }
    if (Build.VERSION.SDK_INT >= 35) {
        try {
            val builder = Notification.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("正在下载魔法包").setProgress(100, progress, false).setOngoing(true)
            val progressStyleClass = Class.forName("android.app.Notification\$ProgressStyle")
            val progressStyle = progressStyleClass.getDeclaredConstructor().newInstance()
            progressStyleClass.getMethod("setTarget", Long::class.java).invoke(progressStyle, 100L)
            progressStyleClass.getMethod("setProgress", Long::class.java).invoke(progressStyle, progress.toLong())
            builder.setStyle(progressStyle as Notification.Style)
            builder.javaClass.getMethod("setRequestPromotedOngoing", Boolean::class.javaPrimitiveType).invoke(builder, true)
            builder.javaClass.getMethod("setShortCriticalText", CharSequence::class.java).invoke(builder, "$progress%")
            manager.notify(notificationId, builder.build())
            return
        } catch (e: Exception) { e.printStackTrace() }
    }
    val compatBuilder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setContentTitle("下载进度: $progress%").setProgress(100, progress, false).setOngoing(true).setSilent(true)
    val notification = compatBuilder.build()
    notification.extras.apply {
        putInt("com.oplus.status_bar_tips_type", 1)
        putInt("com.oplus.progress_value", progress)
    }
    manager.notify(notificationId, notification)
}

// ==========================================
// 🌊 UI 组件层 (修正编译错误后的 Markdown 渲染)
// ==========================================

@Composable
fun MarkdownText(content: String, textColor: Color = MaterialTheme.colorScheme.onSurface) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    val annotatedString = buildAnnotatedString {
        content.split("\n").forEach { rawLine ->
            val line = rawLine.trimEnd()
            when {
                // 三级标题
                line.startsWith("### ") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = primaryColor)) { 
                        append("■ ${line.drop(4)}\n") 
                    }
                }
                // 二级标题
                line.startsWith("## ") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Black, fontSize = 18.sp, color = textColor)) { 
                        append("◈ ${line.drop(3)}\n") 
                    }
                }
                // GitHub 引用块
                line.startsWith("> ") -> {
                    withStyle(SpanStyle(color = textColor.copy(alpha = 0.6f), fontStyle = FontStyle.Italic)) {
                        append("▎${line.drop(2)}\n")
                    }
                }
                // 列表
                line.startsWith("* ") || line.startsWith("- ") -> { 
                    withStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold)) { append("  • ") }
                    append("${line.drop(2)}\n") 
                }
                // 简单处理加粗 **text** (修正编译报错的逻辑)
                line.contains("**") -> {
                    val parts = line.split("**")
                    parts.forEachIndexed { index, part ->
                        if (index % 2 == 1) {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = secondaryColor)) { append(part) }
                        } else {
                            append(part)
                        }
                    }
                    append("\n")
                }
                else -> {
                    if (line.isNotEmpty()) append("$line\n")
                }
            }
        }
    }
    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodySmall,
        lineHeight = 22.sp,
        color = textColor.copy(alpha = 0.85f)
    )
}

@Composable
fun UpdateDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val manager = BakaDownloadManager
    val apkFile = remember { File(context.cacheDir, "baka_update.apk") }
    val tmpFile = remember { File(context.cacheDir, "baka_update.tmp") }
    val prefs = remember { context.getSharedPreferences("ui_settings", Context.MODE_PRIVATE) }
    val useBlurEffect = remember { prefs.getBoolean("use_blur_effect", true) }
    var isChecking by remember { mutableStateOf(true) }
    var updateInfo by remember { mutableStateOf<UpdateData?>(null) }
    
    val currentLocalVersion = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "未知"
        } catch (e: Exception) { "未知" }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        withContext(Dispatchers.IO) {
            try {
                val url = URL("https://api.github.com/repos/wfc35286/baka-update/releases/latest")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    setRequestProperty("Accept", "application/vnd.github.v3+json")
                    setRequestProperty("User-Agent", "Roxy-System")
                }
                if (conn.responseCode == 200) {
                    val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                    val tag = json.getString("tag_name")
                    val assets = json.getJSONArray("assets")
                    var dUrl = ""; var size = 0L
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        if (asset.getString("name").endsWith(".apk")) {
                            dUrl = asset.getString("browser_download_url"); size = asset.getLong("size"); break
                        }
                    }
                    updateInfo = UpdateData(tag, json.getString("name"), json.optString("body", ""), dUrl, 
                        Regex("sha256:([A-Fa-f0-9]{64})").find(json.optString("body"))?.groupValues?.get(1) ?: "",
                        isNewerVersion(currentLocalVersion, tag), size)
                }
            } catch (e: Exception) { e.printStackTrace() }
            isChecking = false
        }
    }

    Dialog(
        onDismissRequest = { if (manager.status != DownloadStatus.DOWNLOADING) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            dialogWindow?.let { window ->
                if (useBlurEffect && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    window.attributes.blurBehindRadius = 110 
                    window.attributes = window.attributes
                    window.setDimAmount(0.45f)
                } else {
                    window.setDimAmount(0.6f)
                }
            }
        }

        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.88f).padding(20.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 12.dp
            ) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        IconButton(onClick = { tmpFile.delete(); apkFile.delete(); manager.status = DownloadStatus.IDLE; manager.progress = 0f }) {
                            Icon(Icons.Default.Delete, "Clear", tint = MaterialTheme.colorScheme.error)
                        }
                        Text("魔法同步", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        IconButton(onClick = onDismiss, enabled = manager.status != DownloadStatus.DOWNLOADING) {
                            Icon(Icons.Default.Close, "Close")
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    if (isChecking) {
                        CircularProgressIndicator(modifier = Modifier.size(40.dp), strokeWidth = 4.dp)
                    } else {
                        updateInfo?.let { info ->
                            Text(info.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                            
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                Text("本地: $currentLocalVersion", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                                Text(" ➔ ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                Text("云端: ${info.version}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                            
                            Spacer(Modifier.height(16.dp))
                            
                            Box(Modifier.fillMaxWidth().heightIn(max = 240.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .padding(14.dp).verticalScroll(rememberScrollState())) {
                                MarkdownText(info.changelog)
                            }
                            
                            if (manager.status == DownloadStatus.DOWNLOADING || manager.status == DownloadStatus.PAUSED) {
                                Spacer(Modifier.height(20.dp))
                                Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), Arrangement.SpaceBetween, Alignment.Bottom) {
                                    Column {
                                        Text(manager.speedText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        Text(manager.sizeText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                    }
                                    Text("${(manager.progress * 100).toInt()}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                                }
                                LinearProgressIndicator(progress = { manager.progress }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape))
                            }
                            
                            Spacer(Modifier.height(24.dp))
                            
                            if (!info.isNewVersion) {
                                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                                    Text("已是最新魔法版本")
                                }
                            } else {
                                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
                                    if (manager.status == DownloadStatus.DOWNLOADING) {
                                        OutlinedButton(onClick = { manager.stop() }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                                            Text("暂停下载")
                                        }
                                    } else {
                                        Button(
                                            onClick = { 
                                                if (manager.status == DownloadStatus.COMPLETED) installApk(context, apkFile)
                                                else manager.start(context, info, tmpFile, apkFile)
                                            }, 
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(14.dp)
                                        ) {
                                            Text(when(manager.status) {
                                                DownloadStatus.COMPLETED -> "立即安装"
                                                DownloadStatus.PAUSED -> "继续同步"
                                                else -> "开始同步"
                                            })
                                        }
                                    }
                                }
                            }
                        } ?: Text("未发现同步契约", color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}
