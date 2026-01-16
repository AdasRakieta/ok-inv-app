plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp") // Stosujemy KSP, wersja będzie w głównym pliku build.gradle
    id("kotlin-parcelize")
    id("androidx.navigation.safeargs.kotlin")
}

android {
    compileSdkVersion(31)

    defaultConfig {
        applicationId = "com.ok.inv"
        minSdkVersion(26)
        targetSdkVersion(31)
            versionCode = 3
            versionName = "0.2.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file("inventory-release.keystore")
            storePassword = "inventory2024"
            keyAlias = "inventory-key"
            keyPassword = "inventory2024"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
    }

    packagingOptions {
        exclude("META-INF/DEPENDENCIES")
        exclude("META-INF/NOTICE*")
        exclude("META-INF/LICENSE*")
        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
    }
}

dependencies {
    // Wersje dostosowane do Gradle 6.7.1
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.appcompat:appcompat:1.4.1")
    implementation("com.google.android.material:material:1.5.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.3")

    // Lifecycle & ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.4.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.4.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.4.1")

    // Navigation Component
    implementation("androidx.navigation:navigation-fragment-ktx:2.4.1")
    implementation("androidx.navigation:navigation-ui-ktx:2.4.1")

    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.2.1")

    // Room z KSP
    val roomVersion = "2.4.2" // Stabilna wersja z KSP
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion") // Używamy KSP

    // ML Kit
    implementation("com.google.mlkit:barcode-scanning:17.0.2")

    // CameraX
    val cameraXVersion = "1.1.0"
    implementation("androidx.camera:camera-core:$cameraXVersion")
    implementation("androidx.camera:camera-camera2:$cameraXVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraXVersion")
    implementation("androidx.camera:camera-view:1.1.0-beta01")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.0")

    // Gson
    implementation("com.google.code.gson:gson:2.8.9")

    // OkHttp for Google Sheets API integration
    implementation("com.squareup.okhttp3:okhttp:4.9.3")

    // ZXing
    implementation("com.google.zxing:core:3.4.1")

    // Testy
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:4.4.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
    testImplementation("androidx.room:room-testing:$roomVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.0")
    testImplementation("androidx.arch.core:core-testing:2.1.0")

    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
    androidTestImplementation("androidx.test:runner:1.4.0")
    androidTestImplementation("androidx.test:rules:1.4.0")

    // Zależności Zebra (poprawione ścieżki)
    implementation(files("ok_mobile_zebra_printer/android/libs/ZSDK_ANDROID_API.jar"))
    implementation(files("ok_mobile_zebra_printer/android/libs/commons-io-2.2.jar"))
    implementation(files("ok_mobile_zebra_printer/android/libs/commons-lang3-3.4.jar"))
    implementation(files("ok_mobile_zebra_printer/android/libs/commons-net-3.1.jar"))
    implementation(files("ok_mobile_zebra_printer/android/libs/commons-validator-1.4.0.jar"))
    implementation(files("ok_mobile_zebra_printer/android/libs/core-1.53.0.0.jar"))
    implementation(files("ok_mobile_zebra_printer/android/libs/httpcore-4.3.1.jar"))
    implementation(files("ok_mobile_zebra_printer/android/libs/httpmime-4.3.2.jar"))
    implementation(files("ok_mobile_zebra_printer/android/libs/jackson-annotations-2.2.3.jar"))
    implementation(files("ok_mobile_zebra_printer/android/libs/jackson-core-2.2.3.jar"))
    implementation(files("ok_mobile_zebra_printer/android/libs/jackson-databind-2.2.3.jar"))
    implementation(files("ok_mobile_zebra_printer/android/libs/opencsv-2.2.jar"))
    implementation(files("ok_mobile_zebra_printer/android/libs/pkix-1.53.0.0.jar"))
    implementation(files("ok_mobile_zebra_printer/android/libs/prov-1.53.0.0.jar"))
    implementation(files("ok_mobile_zebra_printer/android/libs/snmp6_1z.jar"))
}

// Niestandardowe zadania (poprawione ścieżki)
tasks.register("deployDebug") {
    group = "custom"
    description = "Build, install and launch debug APK on connected device"
    dependsOn("installDebug")
    doLast {
        exec {
            workingDir = project.rootDir
            commandLine("cmd", "/c", "C:/Users/%USERNAME%/AppData/Local/Android/Sdk/platform-tools/adb.exe", "shell", "am", "start", "-n", "com.ok.inv/com.example.inventoryapp.ui.main.SplashActivity")
        }
    }
}

tasks.register("quickDeploy") {
    group = "custom"
    description = "Quick build and install debug APK on connected device (no clean)"
    dependsOn("assembleDebug", "installDebug")
}

tasks.register("runOnDevice") {
    group = "custom"
    description = "Launch app on connected device (assumes app is already installed)"
    doLast {
        exec {
            workingDir = project.rootDir
            commandLine("cmd", "/c", "C:/Users/%USERNAME%/AppData/Local/Android/Sdk/platform-tools/adb.exe", "shell", "am", "start", "-n", "com.ok.inv/com.example.inventoryapp.ui.main.SplashActivity")
        }
    }
}

tasks.register("deployRelease") {
    group = "custom"
    description = "Build, install and launch RELEASE APK on connected device"
    dependsOn("installRelease")
    doLast {
        exec {
            workingDir = project.rootDir
            commandLine("cmd", "/c", "C:/Users/%USERNAME%/AppData/Local/Android/Sdk/platform-tools/adb.exe", "shell", "am", "start", "-n", "com.ok.inv/com.example.inventoryapp.ui.main.SplashActivity")
        }
    }
}

tasks.register("quickDeployRelease") {
    group = "custom"
    description = "Quick build and install RELEASE APK on connected device (no clean)"
    dependsOn("assembleRelease", "installRelease")
}
