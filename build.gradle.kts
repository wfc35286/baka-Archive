// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // 统一使用 libs.versions.toml 里的版本，彻底解决 8.7.3 冲突
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
