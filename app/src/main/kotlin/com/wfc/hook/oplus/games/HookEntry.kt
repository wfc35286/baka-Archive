package com.wfc.hook.oplus.games

import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.factory.configs
import com.highcapable.yukihookapi.hook.factory.encase
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.factory.toClass
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.type.java.BooleanType
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit
import com.wfc.hook.oplus.games.Hooker.AutoCombeHook
import com.wfc.hook.oplus.games.Hooker.SkinHook

@InjectYukiHookWithXposed
class HookEntry : IYukiHookXposedInit {

    companion object {
        private const val MODULE_PACKAGE = "com.wfc.hook.oplus.games"
        private const val TARGET_PACKAGE = "com.oplus.games"
    }

    override fun onInit() = configs {
        isDebug = true
        isEnableHookSharedPreferences = true
    }

    override fun onHook() {
        encase {
            // 1. Hook 自己的模块，用于激活检测
            // 使用新版写法：直接定位 Class -> Method -> Hook
            loadApp(name = MODULE_PACKAGE) {
                "$MODULE_PACKAGE.ModuleCheck".toClass()
                    .method {
                        name = "isActive"
                        emptyParam()
                        returnType = BooleanType
                    }
                    .hook {
                        // 直接替换返回值，更加直观
                        replaceTo(true)
                    }
                
                YLog.debug("✅ 模块激活检测 Hook 成功")
            }

            // 2. Hook 目标应用 (游戏助手)
            loadApp(name = TARGET_PACKAGE) {
                YLog.debug("🎮 检测到目标应用: $packageName")

                // --- 核心功能加载 ---
                
                // 加载自动连招功能
                try {
                    AutoCombeHook.hook(this)
                    YLog.debug("✅ 自动连招 Hook 已加载")
                } catch (e: Exception) {
                    YLog.error("❌ 自动连招 Hook 加载失败: ${e.message}")
                }

                // 加载皮肤功能
                try {
                    SkinHook.hook(this)
                    YLog.debug("✅ 皮肤解锁 Hook 已加载")
                } catch (e: Exception) {
                    YLog.error("❌ 皮肤解锁 Hook 加载失败: ${e.message}")
                }
            }
        }
    }
}
