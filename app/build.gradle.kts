plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.smartcommunicator"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.smartcommunicator"
        minSdk = 24
        targetSdk = 36
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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // ===================================================================
    // THE BUG FIX: Overriding the buggy navigation library version.
    // We are explicitly changing from the broken version to a stable one.
    implementation("androidx.navigation:navigation-fragment:2.7.7")
    implementation("androidx.navigation:navigation-ui:2.7.7")
    // ===================================================================

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // these lines for CameraX
    val cameraVersion = "1.3.1"
    implementation("androidx.camera:camera-core:${cameraVersion}")
    implementation("androidx.camera:camera-camera2:${cameraVersion}")
    implementation("androidx.camera:camera-lifecycle:${cameraVersion}")
    implementation("androidx.camera:camera-view:${cameraVersion}")

    // Add this line for ML Kit Text Recognition
    implementation("com.google.mlkit:text-recognition:16.0.0")
    implementation("androidx.preference:preference-ktx:1.2.1")

}
