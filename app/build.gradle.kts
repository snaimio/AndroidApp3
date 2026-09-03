plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.sheikhnaim.sensortoolbox"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sheikhnaim.sensortoolbox"
        minSdk = 24
        targetSdk = 35
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

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Google Play Services - Location (GPS)
    implementation(libs.play.services.location)

    // CardView - For dashboard cards
    implementation(libs.androidx.cardview)

    // RecyclerView - For efficient dashboard
    implementation(libs.androidx.recyclerview)

    // For Map (osmdroid)
    implementation("org.osmdroid:osmdroid-android:6.1.20")

    // Material dependency
    implementation("com.google.android.material:material:1.12.0")

}