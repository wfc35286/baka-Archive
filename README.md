我现在有一个 Android Compose 项目，使用了 Product Flavors 分发版本。
项目在 build.gradle.kts 中定义了不同的变体，并通过 BuildConfig 提供了权限开关。

【项目背景信息】：
1. 变体维度：flavorDimensions += "tier"
2. 版本区分：分为 "standard" (标准版) 和 "pro" (专业版)
3. 权限判断：使用 BuildConfig.IS_PRO (Boolean) 进行逻辑控制
4. 基础包名：com.wfc.hook.oplus.games

【我的需求】：
请帮我实现一个 [此处填写你的新功能名称，例如：自定义背景图功能]。

【实现要求】：
1. 逻辑分层：请根据 BuildConfig.IS_PRO 进行功能拦截。
   - 如果是标准版：[此处填写限制逻辑，例如：只能选择默认的 3 张背景，点击其他背景弹出 Toast 提示升级 Pro]。
   - 如果是专业版：[此处填写解锁逻辑，例如：允许用户从相册自定义上传背景]。
2. UI表现：如果功能受限，请在 UI 上展示“锁”图标或“Pro”标识。
3. 代码规范：请提供全量的 Kotlin 代码，并确保 Import 路径正确（使用 com.wfc.hook.oplus.games.BuildConfig）。
///////呵呵