// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    // Definiowanie wersji dla całego projektu przy użyciu składni Kotlin DSL
    val gradlePluginVersion by extra("4.2.2")
    val kotlinVersion by extra("1.6.21")
    val kspVersion by extra("1.6.21-1.0.6") // Wersja KSP kompatybilna z Kotlin 1.6.21
    val navigationVersion by extra("2.4.1") // Wersja Navigation Component pasująca do reszty

    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:$gradlePluginVersion")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:$kspVersion")
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:$navigationVersion")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

// Poprawiona składnia zadania 'clean' dla Kotlin DSL
tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}
