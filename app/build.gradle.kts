plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

}

android {
    namespace = "com.firstapp.studentguide"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.firstapp.studentguide"
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

            buildConfigField("String", "API_KEY", "\"${System.getenv("API_KEY") ?: project.properties["API_KEY"]}\"")

            buildConfigField ("String", "CITIES_URL", "\"${System.getenv("CITIES_URL") ?: project.properties["CITIES_URL"]}\"")
        }
        debug {

            buildConfigField("String", "API_KEY", "\"${System.getenv("API_KEY") ?: project.properties["API_KEY"]}\"")

            buildConfigField ("String", "CITIES_URL", "\"${System.getenv("CITIES_URL") ?: project.properties["CITIES_URL"]}\"")

        }
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

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    implementation(libs.firebase.auth)


    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.firebase.database)
    implementation(libs.firebase.firestore)
    implementation(libs.volley)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)

    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
    implementation("com.google.guava:guava:33.4.6-android")
    implementation("org.reactivestreams:reactive-streams:1.0.4")

    implementation ("com.google.mlkit:translate:17.0.3")
    implementation (libs.material.v1110)
    implementation("com.squareup.okhttp3:okhttp:4.9.0")

}