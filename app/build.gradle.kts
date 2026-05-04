import java.util.Properties
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("org.jetbrains.kotlin.kapt")

}

android {
    namespace = "com.ritesh.hoppeconnect"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ritesh.hoppeconnect"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val localProps = Properties()
        val localFile = rootProject.file("local.properties")
        if (localFile.exists()) localProps.load(localFile.inputStream())

        buildConfigField("String", "APPWRITE_PROJECT_ID",       "\"${localProps["APPWRITE_PROJECT_ID"]}\"")
        buildConfigField("String", "APPWRITE_DB_ID",            "\"${localProps["APPWRITE_DB_ID"]}\"")
        buildConfigField("String", "APPWRITE_USERS_BUCKET_ID",  "\"${localProps["APPWRITE_USERS_BUCKET_ID"]}\"")
        buildConfigField("String", "APPWRITE_REPORT_BUCKET_ID", "\"${localProps["APPWRITE_REPORT_BUCKET_ID"]}\"")
        buildConfigField("String", "APPWRITE_CHAT_BUCKET_ID",   "\"${localProps["APPWRITE_CHAT_BUCKET_ID"]}\"")
        buildConfigField("String", "ADMIN_EMAIL",               "\"${localProps["ADMIN_EMAIL"]}\"")
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

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

configurations.all {
    resolutionStrategy {
        force("io.appwrite:sdk-for-android:5.1.0")
        force("com.squareup.okhttp3:okhttp:4.12.0")
    }
}

dependencies {
    implementation("io.appwrite:sdk-for-android:5.1.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.com.google.android.material)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.lifecycle.runtime)

    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    implementation("com.github.bumptech.glide:glide:4.16.0")
    kapt("com.github.bumptech.glide:compiler:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.google.zxing:core:3.5.3")

    implementation("com.google.android.gms:play-services-auth:21.0.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")

    implementation("com.google.mlkit:face-detection:16.1.5")
    implementation("org.tensorflow:tensorflow-lite:2.14.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}