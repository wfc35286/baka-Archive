# Update 页面混淆策略（简洁版）

## 数据类
- UpdateData
  - 保留 JSON 字段: version, title, changelog, downloadUrl, remoteSha256
  - 类名/方法可混淆

## 下载管理器
- BakaDownloadManager
  - 类名和方法可混淆
  - 保留反射相关:
    - Notification$ProgressStyle
    - setRequestPromotedOngoing
    - setShortCriticalText
  - 状态字段可混淆或加密处理

## 工具方法
- isNewerVersion / getFileSha256
  - 可混淆
- installApk
  - 保留 FileProvider 相关反射
- updateLiveNotification
  - 保留 Notification 反射部分
  - 其余可混淆

## UI 层 (Compose)
- UpdateDialog / MarkdownText
  - 保留 @Composable 注解
  - 内部逻辑方法可混淆
  - MaterialTheme color / UI 样式可混淆类名，但保留函数调用
- MarkdownText
  - 函数名可混淆
  - 字符串解析方法可混淆

## 网络 /权限
- GitHub JSON key: tag_name, name, body, assets → 保留
- URL / request 方法可混淆
- POST_NOTIFICATIONS 权限无需特殊处理

## 总结
- 保留: @Composable、JSON关键字段、反射调用类/方法、FileProvider/Notification反射
- 可混淆: UI内部函数、逻辑类名/方法、非关键状态变量
- 注意: JSON key和反射必须保留，否则解析/通知/安装可能出错


# SkinHackScreen 页面混淆建议

## 保留（-keep）
- @Composable 函数：SkinHackScreen
- RootUtils 内调用方法：readSkinConfig, saveSkinConfig, restartGameHelper
- Drawable 资源名称

## 可混淆
- 内部变量：activeSkin, scaleAnim, scrollState, skinList 等
- 局部函数、lambda
- UI 内部状态和动画参数

## 注意
- 混淆 key / 内部标识符可以，但不要混淆 UI 显示文本
- 反射或动态资源调用（getIdentifier）不能混淆资源名
- Compose 注解必须保留

# AutoCombeScreen 页面混淆建议

## 保留（-keep）
- @Composable 函数：AutoCombeScreen
- RootUtils 内核心方法：readComboConfig, saveComboConfig, restartGameHelper
- UI 显示文本和图标资源

## 可混淆
- 内部状态变量：maxText, scrollState
- 局部函数和 lambda
- UI 内部动画、padding、圆角变量

## 注意
- Compose 注解必须保留
- 按钮文本、占位值 999、图标资源不要混淆
- lambda 内逻辑可混淆，但调用 RootUtils 方法时不要改名字


# HomeScreen 页面混淆建议

## 保留（-keep）
- @Composable 函数：HomeScreen, MainListContent, FeatureCard
- NavHost route 字符串："list", "feature_number", "feature_skin"
- 引用页面函数：AutoCombeScreen, SkinHackScreen
- UI 文本和图标资源

## 可混淆
- 内部变量：features, themePrimary, feature, padding 等
- 局部函数和 lambda
- 数据类 FeatureItem 的类名和非 route 字段

## 注意
- route 字符串和 UI 显示文本不要改
- Compose 注解必须保留
- 系统图标不用混淆

# 混淆 RootUtils
-keep class com.wfc.hook.oplus.games.RootUtils { 
    <methods>; 
    <fields>;
}

# 混淆 ModuleCheck
-keep class com.wfc.hook.oplus.games.ModuleCheck { 
    <methods>; 
    <fields>; 
}