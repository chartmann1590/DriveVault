import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.firebase.crashlytics")
    id("com.google.firebase.firebase-perf")
    id("com.google.gms.google-services")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

fun localProp(name: String, defaultValue: String = ""): String =
    localProperties.getProperty(name, defaultValue)

android {
    namespace = "com.drivevault.dashcam"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.drivevault.dashcam"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        multiDexEnabled = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }

        buildConfigField("String", "FIREBASE_PROJECT_ID", "\"${localProp("FIREBASE_PROJECT_ID")}\"")
        buildConfigField("String", "FIREBASE_PROJECT_NUMBER", "\"${localProp("FIREBASE_PROJECT_NUMBER")}\"")

        buildConfigField("String", "GITHUB_API_TOKEN", "\"${localProp("github.api.token")}\"")
        buildConfigField("String", "GITHUB_REPO_OWNER", "\"${localProp("github.repo.owner")}\"")
        buildConfigField("String", "GITHUB_REPO_NAME", "\"${localProp("github.repo.name")}\"")
        buildConfigField("String", "FEEDBACK_ASSETS_DIR", "\"feedback-assets\"")
        buildConfigField("String", "SUPABASE_URL", "\"${localProp("SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${localProp("SUPABASE_ANON_KEY")}\"")
        buildConfigField("String", "SUPABASE_SERVICE_ROLE_KEY", "\"${localProp("SUPABASE_SERVICE_ROLE_KEY")}\"")
        buildConfigField(
            "String",
            "SHARE_VIEWER_URL",
            "\"${localProp("SHARE_VIEWER_URL", "https://chartmann1590.github.io/DriveVault")}\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField("String", "FIREBASE_APP_ID", "\"${localProp("FIREBASE_RELEASE_APP_ID")}\"")
            buildConfigField("String", "FIREBASE_API_KEY", "\"${localProp("FIREBASE_RELEASE_API_KEY")}\"")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            buildConfigField("String", "FIREBASE_APP_ID", "\"${localProp("FIREBASE_DEBUG_APP_ID")}\"")
            buildConfigField("String", "FIREBASE_API_KEY", "\"${localProp("FIREBASE_DEBUG_API_KEY")}\"")
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
        sarifReport = true
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
        buildConfig = true
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
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-service:2.7.0")
    implementation("androidx.lifecycle:lifecycle-process:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.navigation:navigation-compose:2.7.6")

    implementation("androidx.camera:camera-core:1.4.2")
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    implementation("androidx.camera:camera-video:1.4.2")
    implementation("androidx.camera:camera-view:1.4.2")

    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-ui:1.2.1")
    implementation("androidx.media3:media3-common:1.2.1")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")

    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    implementation("androidx.core:core-splashscreen:1.0.1")

    implementation("io.coil-kt:coil-compose:2.5.0")

    implementation("com.google.mlkit:object-detection:17.0.2")

    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-perf")
    implementation("com.google.firebase:firebase-config")
    implementation("com.google.firebase:firebase-messaging")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}

// Generate google-services.json from local.properties so the Google Services Gradle
// plugin can resolve the Firebase project without committing the file to Git.
// Both debug and release clients are included; the plugin selects the matching
// package name at build time.
val applicationIdValue: String = android.defaultConfig.applicationId!!

tasks.register("generateGoogleServicesJson") {
    val outputFile = layout.projectDirectory.file("google-services.json").asFile
    outputs.file(outputFile)

    doLast {
        val projectId = localProp("FIREBASE_PROJECT_ID")
        val projectNumber = localProp("FIREBASE_PROJECT_NUMBER")
        val storageBucket = localProp("FIREBASE_STORAGE_BUCKET")
        val debugAppId = localProp("FIREBASE_DEBUG_APP_ID")
        val debugApiKey = localProp("FIREBASE_DEBUG_API_KEY")
        val releaseAppId = localProp("FIREBASE_RELEASE_APP_ID")
        val releaseApiKey = localProp("FIREBASE_RELEASE_API_KEY")

        outputFile.writeText(
            """
            {
              "project_info": {
                "project_number": "$projectNumber",
                "firebase_url": "",
                "project_id": "$projectId",
                "storage_bucket": "$storageBucket"
              },
              "client": [
                {
                  "client_info": {
                    "mobilesdk_app_id": "$debugAppId",
                    "android_client_info": {
                      "package_name": "${applicationIdValue}.debug"
                    }
                  },
                  "api_key": [
                    { "current_key": "$debugApiKey" }
                  ]
                },
                {
                  "client_info": {
                    "mobilesdk_app_id": "$releaseAppId",
                    "android_client_info": {
                      "package_name": "$applicationIdValue"
                    }
                  },
                  "api_key": [
                    { "current_key": "$releaseApiKey" }
                  ]
                }
              ]
            }
            """.trimIndent()
        )
    }
}

// NOTE: google-services.json must exist before Gradle configures the project.
// Run `./gradlew generateGoogleServicesJson` whenever local.properties changes,
// or generate it in CI before invoking Gradle (see .github/workflows/build.yml).
