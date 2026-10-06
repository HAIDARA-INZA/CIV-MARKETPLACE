plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    id("com.google.gms.google-services")
}

android {
    namespace = "ci.devsphere.civmarketplace"
    compileSdk = 36

    defaultConfig {
        applicationId = "ci.devsphere.civmarketplace"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "BASE_API_URL", "\"https://haidara.devsphere.ci/laravel/public/api/\"")
        buildConfigField("String", "PUSHER_KEY", "\"11d99e0de92fae8a152f\"")
        buildConfigField("String", "PUSHER_CLUSTER", "\"eu\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            optimization {
                enable = true
            }
        }
    }
    androidResources {
        noCompress += "png"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation(libs.androidx.lifecycle.process)
    implementation("androidx.compose.material:material-icons-extended:1.7.0")
    implementation(libs.androidx.browser)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Navigation
    implementation(libs.navigation.compose)

    // Retrofit & Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging)

    // Coil (Images)
    implementation(libs.coil.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Pusher
    implementation(libs.pusher.client)

    // Firebase Messaging
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    // Background sync/offline retry + app badge
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("me.leolin:ShortcutBadger:1.1.22")

    // DataStore
    implementation(libs.datastore.preferences)
    
    // System UI Controller (Edge-to-Edge)
    implementation(libs.accompanist.systemuicontroller)

    // AppCompat (pour AppCompatDelegate.setApplicationLocales — changement de langue)
    implementation("androidx.appcompat:appcompat:1.7.0")

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

