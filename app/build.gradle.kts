plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp") // ✅ 既にある
}

android {
    namespace = "com.github.intent.pad"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.github.intent.pad"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
        // ✅ 実験的APIを許可する設定（既にある）
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=kotlin.RequiresOptIn"
        )
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Activity & Core
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Google Material (XMLテーマ用)
    implementation("com.google.android.material:material:1.11.0")

    // Room Database - ✅ 既にあるのでそのまま
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // GSON (インポート/エクスポート用)
    implementation("com.google.code.gson:gson:2.10.1")

    // ✅ 追加するのはこの1行だけ！
    // 画像読み込み用 (Coil) - 画像アイコン機能に必要
    implementation("io.coil-kt:coil-compose:2.6.0")
}
