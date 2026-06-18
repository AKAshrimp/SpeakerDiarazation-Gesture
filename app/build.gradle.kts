plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "hk.edu.hkmu.speakerdiarazationdemo"
    compileSdk = 36

    defaultConfig {
        applicationId = "hk.edu.hkmu.speakerdiarazationdemo"
        minSdk = 28
        targetSdk = 28
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    packaging {
        jniLibs {
            // Work around 16 KB page-size / APK native-load alignment issues on some devices.
            useLegacyPackaging = true
        }
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Gesture recognition module (Kotlin)
    implementation(project(":gesture"))
    
    // OkHttp for WebSocket
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    
    // Gson for JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
