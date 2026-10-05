plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "br.com.helldiversbr.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "br.com.helldiversbr.app"
        minSdk = 26
        targetSdk = 34
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Incremente versionCode e versionName a cada novo APK publicado.
        // O app compara versionCode com o valor de versao-app.json para avisar de atualizações.
        versionCode = 37
        versionName = "37.0.0"
    }

    signingConfigs {
        create("permanent") {
            System.getenv("HD_SIGNING_STORE_FILE")?.takeIf { it.isNotBlank() }?.let {
                storeFile = file(it)
            }
            storePassword = System.getenv("HD_SIGNING_PASSWORD")
            keyAlias = "hd2-br"
            keyPassword = System.getenv("HD_SIGNING_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("permanent")
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
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-svg:2.7.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}

// Impede publicar APK Release sem a chave permanente configurada.
tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    doFirst {
        val store = System.getenv("HD_SIGNING_STORE_FILE")
        require(!store.isNullOrBlank() && file(store).isFile) {
            "Configure HD_SIGNING_STORE_FILE com a chave permanente."
        }
        require(!System.getenv("HD_SIGNING_PASSWORD").isNullOrBlank()) {
            "Configure HD_SIGNING_PASSWORD para compilar o APK Release."
        }
    }
}
