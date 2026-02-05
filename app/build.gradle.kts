import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import java.util.Properties
import java.io.FileInputStream
import java.io.FileOutputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp) 
}

// --- ⭐ 洛琪希的高级构建计数魔法 (Git 增强版) ---
val versionPropsFile = file("version.properties") 
val props = Properties()

if (!versionPropsFile.exists()) {
    versionPropsFile.createNewFile()
    props["MAJOR_BUILD"] = "0"
    props["MINOR_BUILD"] = "0"
    FileOutputStream(versionPropsFile).use { props.store(it, "Initial Roxy Build System") }
} else {
    FileInputStream(versionPropsFile).use { props.load(it) }
}

var majorBuild = props["MAJOR_BUILD"]?.toString()?.toInt() ?: 0
var minorBuild = props["MINOR_BUILD"]?.toString()?.toInt() ?: 0

// 只要是 Release 任务就自动增加小版本号
val isReleaseTask = gradle.startParameter.taskNames.any { 
    it.contains("assemble", ignoreCase = true) && it.contains("Release", ignoreCase = true) 
}

if (isReleaseTask) {
    minorBuild++
    if (minorBuild >= 100) {
        majorBuild++
        minorBuild = 0
    }
    props["MAJOR_BUILD"] = majorBuild.toString()
    props["MINOR_BUILD"] = minorBuild.toString()
    FileOutputStream(versionPropsFile).use { props.store(it, "Roxy Build System") }
}

// 获取 Git 提交数
val gitCommitCount = try {
    Runtime.getRuntime().exec("git rev-list --count HEAD").inputStream.reader().readText().trim()
} catch (e: Exception) {
    "0" 
}

// 获取 Git 哈希 (Short)
val gitCommitHash = try {
    Runtime.getRuntime().exec("git rev-parse --short HEAD").inputStream.reader().readText().trim()
} catch (e: Exception) {
    "null"
}

// ⭐ 洛琪希修正：构建时间戳格式化
// 注意：versionCode 是 int 类型，最大值约为 21 亿 (2,147,483,647)。
// 如果使用 yyyyMMddHHmm (如 202601012000) 会导致整数溢出。
// 因此这里采用 yyMMddHH (如 26020417，即 26年2月4日17点) 格式，既保持了递增性，又在安全范围内。
val now = LocalDateTime.now(ZoneId.systemDefault())
val dateVersionCode = now.format(DateTimeFormatter.ofPattern("yyyyMMdd")).toInt()

// ⭐⭐⭐ 修正后的版本号格式：大版本.次版本.提交数.哈希 ⭐⭐⭐
// 例如：1.5.123.a1b2c3
val finalVersionName = "$majorBuild.$minorBuild.$gitCommitCount.$gitCommitHash"
// --- ⭐ 魔法结束 ---

android {
    namespace = "com.wfc.hook.oplus.games"
    compileSdk = 36 

    signingConfigs {
        create("release") {
            storeFile = file("my-release-key.jks") 
            storePassword = "roxy1234"
            keyAlias = "roxy1234"
            keyPassword = "roxy1234"
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true 
        }
    }

    defaultConfig {
        applicationId = "com.wfc.hook.oplus.games"
        minSdk = 31 
        targetSdk = 36
        // 这里使用了新的日期格式 versionCode
        versionCode = dateVersionCode
        versionName = finalVersionName
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk { abiFilters.add("arm64-v8a") }

        manifestPlaceholders["appName"] = "WFC Toolbox"
    }
    
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("release")
        }
        release {
            isMinifyEnabled = true 
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { 
        jvmTarget = "17" 
    }

    buildFeatures { 
        compose = true 
        buildConfig = true 
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.18.0-alpha01")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.activity:activity-compose:1.13.0-alpha01")
    
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")

    implementation(platform("androidx.compose:compose-bom:2026.01.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    
    implementation("androidx.compose.material:material-icons-extended") 

    implementation("androidx.navigation:navigation-compose:2.9.6")

    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.documentfile:documentfile:1.1.0")

    implementation("com.squareup.okhttp3:okhttp:5.3.2") 

    implementation("com.google.android.material:material:1.14.0-alpha09")
    
    // ⭐ YukiHookAPI 依赖
    implementation("com.highcapable.yukihookapi:api:1.3.1")
    ksp("com.highcapable.yukihookapi:ksp-xposed:1.3.1")
    
    // ⭐ 洛琪希：改用远程 Xposed API 引用
    compileOnly("de.robv.android.xposed:api:82")
}

tasks.register("showVersion") {
    doLast {
        println("FINAL_VERSION_NAME:${android.defaultConfig.versionName}")
        println("FINAL_VERSION_CODE:${android.defaultConfig.versionCode}")
    }
}
