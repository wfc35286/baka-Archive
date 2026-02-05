package com.wfc.hook.oplus.games.Hooker

import android.app.Dialog
import android.view.View
import android.view.ViewGroup
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.factory.toClass
import com.highcapable.yukihookapi.hook.param.PackageParam
import com.highcapable.yukihookapi.hook.type.java.StringType
import java.io.File

object AutoCombeHook {
    // 目标类：这个类名通常比较稳定，不太容易变
    private const val TARGET_CLASS = "com.oplus.games.feature.gameautomation.utils.GameAutomationDialogUtil"
    
    // 方法名：'u' 是混淆名，如果更新后失效，只需改这一个字母即可
    private const val TARGET_METHOD_UI = "u"
    
    fun hook(packageParam: PackageParam) {
        with(packageParam) {
            
            // 1. 逻辑 Hook
            TARGET_CLASS.toClass()
                .method { name = "k"; param(StringType) }
                .hook {
                    before {
                        val input = args[0] as? String ?: return@before
                        val inputNum = input.toIntOrNull() ?: return@before
                        
                        // 使用 YukiHookAPI 的 prefs 读取配置
                        val offset = prefs("combo_config").getInt("offset", 1)
                        val maxValue = prefs("combo_config").getInt("max_count", 999)
                        
                        if (inputNum in 1..maxValue) {
                            args[0] = (inputNum + offset).toString()
                            result = true
                        }
                    }
                }

            // 2. UI Hook (通用搜索版)
            TARGET_CLASS.toClass()
                .method {
                    name = TARGET_METHOD_UI
                    paramCount(2) 
                }
                .hook {
                    after {
                        val dialog = args[0] as? Dialog ?: return@after
                        val decorView = dialog.window?.decorView ?: return@after
                        val inputView = findViewBySimpleName(decorView, "COUIInputView")

                        if (inputView != null) {
                            try {
                                val maxValue = prefs("combo_config").getInt("max_count", 999)
                                val newTitle = "请输入 1～$maxValue 内的整数"
                                
                                inputView.javaClass.getMethod("setTitle", CharSequence::class.java)
                                    .invoke(inputView, newTitle)
                            } catch (e: Exception) {
                                // 静默失败
                            }
                        }
                    }
                }
        }
    }

    /**
     * 递归查找 View
     */
    private fun findViewBySimpleName(view: View, targetName: String): View? {
        if (view.javaClass.simpleName.contains(targetName, ignoreCase = true)) {
            return view
        }
        
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                val result = findViewBySimpleName(child, targetName)
                if (result != null) return result
            }
        }
        return null
    }


}
