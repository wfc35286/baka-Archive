package com.wfc.hook.oplus.games

import com.highcapable.yukihookapi.hook.factory.prefs
import java.io.File
import android.content.Context

object RootUtils {
    // --- 路径规范定义 (保留用于清理旧配置) ---
    private const val BASE_DIR = "/data/local/tmp/rkx"
    
    // --- 包名定义 ---
    private const val PKG_GAME_HELPER = "com.oplus.games" // 或者是 com.coloros.gamespaceui，视具体机型而定
    
    /**
     * 检测设备是否具有 Root 权限
     */
    fun checkRoot(): Boolean {
        return try {
            val paths = arrayOf("/system/bin/su", "/system/xbin/su", "/sbin/su")
            paths.any { File(it).exists() } || 
            Runtime.getRuntime().exec("su -c id").waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }
    
    // ========== 自动连招功能 (Combo) ==========
    
    /**
     * 保存连招上限配置
     */
    fun saveComboConfig(context: Context, maxCount: Int): Boolean {
        return try {
            context.prefs("combo_config").edit {
                putInt("offset", 1)
                putInt("max_count", maxCount)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 读取连招配置
     * @return Pair(偏移值, 上限值)
     */
    fun readComboConfig(context: Context): Pair<Int, Int> {
        return try {
            val prefs = context.prefs("combo_config")
            Pair(prefs.getInt("offset", 1), prefs.getInt("max_count", 999))
        } catch (e: Exception) {
            Pair(1, 999)
        }
    }
    
    // ========== 皮肤中心功能 (Skin) ==========
    
    /**
     * 保存激活的皮肤 ID
     */
    fun saveSkinConfig(context: Context, skinId: String): Boolean {
        return try {
            val finalId = if (skinId.isBlank()) "nt" else skinId
            context.prefs("skin_config").edit {
                putString("active_skin", finalId)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 读取当前激活的皮肤 ID
     */
    fun readSkinConfig(context: Context): String {
        return try {
            context.prefs("skin_config").getString("active_skin", "nt")
        } catch (e: Exception) {
            "nt"
        }
    }

    // ========== ✨ 全游戏解锁功能 (Unlock) ==========
    private const val UNLOCK_CFG_FILE = "$BASE_DIR/unlock_config"

    /**
     * 保存全游戏解锁状态
     * @param isEnabled true: 开启伪装(解锁), false: 关闭伪装
     */
    fun saveUnlockConfig(isEnabled: Boolean): Boolean {
        return try {
            // 将布尔值转换为字符串 "true" 或 "false"
            val content = isEnabled.toString()
            
            val script = """
                mkdir -p $BASE_DIR
                echo '$content' > $UNLOCK_CFG_FILE
                chmod 777 $BASE_DIR
                chmod 644 $UNLOCK_CFG_FILE
            """.trimIndent()
            
            executeShell(script)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 读取全游戏解锁状态
     */
    fun readUnlockConfig(): Boolean {
        return try {
            val content = readFileRoot(UNLOCK_CFG_FILE)
            // 只有文件内容明确为 "true" 时才返回 true，否则默认关闭
            content == "true"
        } catch (e: Exception) {
            false
        }
    }
    
    // ========== 核心工具私有实现 ==========
    
    /**
     * 使用 Root 权限执行脚本
     */
    private fun executeShell(script: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", script))
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 使用 Root 权限读取文件内容
     */
    private fun readFileRoot(path: String): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "cat $path"))
            val content = process.inputStream.bufferedReader().use { it.readText() }.trim()
            content
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 强杀并重启游戏助手
     */
    fun restartGameHelper(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "am force-stop $PKG_GAME_HELPER"))
            process.waitFor()
            true 
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 【重置配置】精准删除 rkx 文件夹
     * 洛琪希保证：绝对不会触碰 BASE_DIR 以外的任何文件！
     */
    fun resetAllConfigs(): Boolean {
        return try {
            // 使用 su -c 执行 rm -rf，精准定位到 rkx 目录
            executeShell("rm -rf /data/local/tmp/rkx")
        } catch (e: Exception) {
            false
        }
    }
}

object ModuleCheck {
    fun isActive(): Boolean = false
}
