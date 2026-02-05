package com.wfc.hook.oplus.games.Hooker

import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.factory.toClass
import com.highcapable.yukihookapi.hook.param.PackageParam
import java.io.File

object SkinHook {
    private const val TARGET_CLASS = "com.coloros.gamespaceui.helper.c"
    private const val TARGET_METHOD = "g"
    fun hook(packageParam: PackageParam) {
        with(packageParam) {
            TARGET_CLASS.toClass()
                .method {
                    name = TARGET_METHOD
                    emptyParam() 
                }
                .hook {
                    after {
                        // 使用 YukiHookAPI 的 prefs 读取配置
                        val skin = prefs("skin_config").getString("active_skin", "nt")
                        if (skin != "nt") {
                            result = skin
                        }
                    }
                }
        }
    }
}
