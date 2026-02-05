# ==========================================================
# 1. 基础魔法环境构建 (Base Settings)
# ==========================================================
# 忽略泛型报错，防止构建中断
-dontwarn java.lang.reflect.AnnotatedType
# 保持关键元数据：泛型(Signature)、注解(Annotation)、内部类结构
# 假如没有这些，YukiHook 和 Compose 都会迷路的
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses, SourceFile, LineNumberTable

# ==========================================================
# 2. 核心引擎保护 (YukiHookAPI & KavaRef)
# ==========================================================
# 这些是核心库，动了它们，钩子就挂不上了，必须全量保留
-keep class com.highcapable.yukihookapi.** { *; }
-keep class com.highcapable.kavaref.** { *; }
-keep interface com.highcapable.yukihookapi.** { *; }
-keep interface com.highcapable.kavaref.** { *; }

# ==========================================================
# 3. Xposed 入口与注入 (Injection Points)
# ==========================================================
# 保护实现了初始化接口的类，不然 LSPosed 找不到门
-keep class * implements com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit { *; }

# 显式保护 HookEntry，这是插件的“起手式”
-keep class com.wfc.hook.oplus.games.HookEntry { *; }

# 保护 Application 类，防止被混淆导致 Manifest 找不到类
-keep class com.wfc.hook.oplus.games.application.BakaApplication { *; }

# 保护被 @InjectYukiHookWithXposed 标记的字段，这是注入的关键点
-keepclassmembers class * {
    @com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed *;
}

# ==========================================================
# 4. 关键业务逻辑 (精细化控制)
# ==========================================================
# ⚠️ 注意：我们要删掉原来那个 `-keep class com.wfc.hook.oplus.games.** { *; }`
# 改为只保护被反射或字符串调用的特定类

# [ModuleCheck]
# 你提到 HookEntry 通过字符串查找它，所以类名和所有成员都不能改名
-keep class com.wfc.hook.oplus.games.ModuleCheck { *; }

# [RootUtils]
# 虽然你笔记里写了“混淆 RootUtils”，但下面又写了 -keep。
# 如果 HookEntry 或 UI 层通过反射调用它，必须保留。
# 为了稳妥，保留类名和公开方法，私有逻辑交给 R8 去优化
-keep class com.wfc.hook.oplus.games.RootUtils {
    public <methods>;
    public <fields>;
}

# ==========================================================
# 5. 数据模型与网络 (Update / JSON)
# ==========================================================
# [UpdateData]
# 如果你用 Gson/Moshi，且没有用 @SerializedName 注解，字段名必须保留
# 如果全用了 @SerializedName，这部分其实可以放宽，但为了保险起见：
-keepclassmembers class **.UpdateData {
    java.lang.String version;
    java.lang.String title;
    java.lang.String changelog;
    java.lang.String downloadUrl;
    java.lang.String remoteSha256;
}

# [BakaDownloadManager]
# 保留你提到的反射相关方法，其他部分允许混淆
-keepclassmembers class **.BakaDownloadManager {
    void setRequestPromotedOngoing(boolean);
    void setShortCriticalText(java.lang.String);
}
# 如果 Notification$ProgressStyle 是内部类，且被反射使用
-keep class **.Notification$ProgressStyle { *; }

# ==========================================================
# 6. UI 界面层 (Jetpack Compose)
# ==========================================================
# Compose 的函数本身会被编译器处理，但作为导航入口（Route）的函数名建议保留
# 这样在 Log 中报错也容易定位

# [Screen Functions]
# 保留特定的 Composable 函数名，防止导航路径失效（如果是基于函数名反射路由的话）
-keep class com.wfc.hook.oplus.games.** {
    @androidx.compose.runtime.Composable void SkinHackScreen(...);
    @androidx.compose.runtime.Composable void AutoCombeScreen(...);
    @androidx.compose.runtime.Composable void HomeScreen(...);
    @androidx.compose.runtime.Composable void UpdateDialog(...);
}

# [MarkdownText]
# 如果有反射调用解析逻辑，保留该类
-keep class **.MarkdownText {
    public <methods>;
}

# ==========================================================
# 7. 系统兼容性屏蔽 (Warnings)
# ==========================================================
-dontwarn android.content.pm.IPackageDataObserver
-dontwarn android.content.pm.IPackageDeleteObserver
-dontwarn android.content.pm.IPackageInstallObserver
-dontwarn sun.misc.Unsafe

# ==========================================================
# BonusGalleryScreen (秘密画廊) 专项保护
# ==========================================================

# 1. 强力保护：保留主文件类及其所有成员（包括私有方法、变量）
# 之前的规则只保了入口函数，导致内部的 toSha256, extractUid 和 URL 处理逻辑被 R8 砍掉了
# 星号 * 表示保留该类下的所有东西，确保逻辑完整
-keep class com.wfc.hook.oplus.games.UI.BonusGalleryScreenKt { *; }

# 2. 核心修复：保留所有内部类、Lambda 表达式和协程上下文
# 这一步最重要！LaunchedEffect 和 OkHttp 的异步回调都编译成了内部类（$1, $2...）
# 如果不保留，网络请求的回调就会“断气”，导致图片加载请求发不出去或回不来
-keep class com.wfc.hook.oplus.games.UI.BonusGalleryScreenKt$** { *; }

# 3. 保护沉浸式 Dialog 的系统交互
# 避免 R8 警告并移除沉浸式窗口控制的代码
-dontwarn android.view.Window
-dontwarn androidx.core.view.**