plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.ksp)
}

android {
    namespace = "com.android.toolbag"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.android.toolbag"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

    }

    signingConfigs {
        create("release") {
            storeFile = file("D:\\Android12SignerGUI\\SignFiles\\NewPublic\\platform.jks")
            keyAlias = "android"
            keyPassword = "android"
            storePassword = "android"
        }
    }
    buildTypes {
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidChart)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.baseRecyclerViewAdapterHelper)
    debugImplementation(libs.glance)
    ksp(libs.room.compiler)
}