package com.wfc.hook.oplus.games.application

import com.highcapable.yukihookapi.hook.xposed.application.ModuleApplication

class BakaApplication : ModuleApplication() {
    override fun onCreate() {
        super.onCreate()
        // 这里可以添加其他初始化逻辑
    }
}
