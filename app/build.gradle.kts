plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// 签名信息不进版本库：优先 keystore.properties（已 .gitignore），其次环境变量（CI 用）。
// 注意：签名密钥必须保持不变。换钥匙会让老用户无法覆盖安装升级——
// minSdk = 26，而 APK 签名 v3 的密钥轮换（proof-of-rotation）只在 Android 9+ / API 28+ 生效。
val keystoreProps = java.util.Properties().apply {
    val f = file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.lechenmusic"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.lechenmusic"
        minSdk = 26
        targetSdk = 34
        versionCode = 61
        versionName = "1.6.6"
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        create("release") {
            // 找不到配置时退回相对路径默认值，保证本地已有 keystore.properties 的老工作流不受影响
            storeFile = file(
                keystoreProps.getProperty("storeFile")
                    ?: System.getenv("LECHEN_KEYSTORE_FILE")
                    ?: "release.keystore.p12"
            )
            storePassword = keystoreProps.getProperty("storePassword")
                ?: System.getenv("LECHEN_STORE_PASSWORD")
                ?: ""
            keyAlias = keystoreProps.getProperty("keyAlias")
                ?: System.getenv("LECHEN_KEY_ALIAS")
                ?: "lechenmusic"
            keyPassword = keystoreProps.getProperty("keyPassword")
                ?: System.getenv("LECHEN_KEY_PASSWORD")
                ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
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
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)

    // Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // Network - Retrofit + OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Gson
    implementation("com.google.code.gson:gson:2.10.1")

    // Coil - Image loading
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("androidx.palette:palette-ktx:1.0.0")

    // Media3 ExoPlayer
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.2.1")
    implementation("androidx.media3:media3-session:1.2.1")
    implementation("androidx.media3:media3-ui:1.2.1")
    implementation("androidx.media3:media3-datasource-okhttp:1.2.1")

    // Media compat for notification MediaStyle
    implementation("androidx.media:media:1.7.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Pinyin
    implementation("com.belerweb:pinyin4j:2.5.1")

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
}
