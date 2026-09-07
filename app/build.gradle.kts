plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.farmifyai.agri"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    // Resolution order:
    //   1. -PbackendBaseUrl=... on the Gradle command line (CI / release builds)
    //   2. backendBaseUrl=... in gradle.properties
    //   3. backendBaseUrl=... in app/.env      <- normal local development
    //   4. backendBaseUrl=... in app/.env.example
    //
    // The Secrets Gradle Plugin reads .env into its OWN BuildConfig fields; it does
    // NOT register Gradle properties. Relying on providers.gradleProperty() alone
    // silently produced an empty BACKEND_BASE_URL, which made every cloud call fail
    // and pushed the app onto its offline fallback path.
    // providers.fileContents() is used instead of a plain File read so the
    // configuration cache re-runs this block when app/.env actually changes.
    fun readEnvValue(fileName: String, key: String): String? =
      providers.fileContents(layout.projectDirectory.file(fileName)).asText.orNull
        ?.lineSequence()
        ?.map { it.trim() }
        ?.firstOrNull { !it.startsWith("#") && it.startsWith("$key=") }
        ?.substringAfter("=")
        ?.trim()
        ?.trim('"', '\'')
        ?.takeIf { it.isNotBlank() && !it.contains("your-project") && !it.contains("your-backend") }

    val backendBaseUrl = providers.gradleProperty("backendBaseUrl").orNull?.trim()?.takeIf { it.isNotBlank() }
      ?: readEnvValue(".env", "backendBaseUrl")
      ?: ""

    if (backendBaseUrl.isBlank() || !backendBaseUrl.startsWith("https://")) {
      logger.warn(
        "FarmifyAI: backendBaseUrl is not set to an https:// URL. " +
          "The app will build but every cloud feature (auth, sync, chat, mandi) will be disabled. " +
          "Set it in app/.env or pass -PbackendBaseUrl=https://your-backend"
      )
    } else {
      logger.lifecycle("FarmifyAI: backend = $backendBaseUrl")
    }

    buildConfigField("String", "BACKEND_BASE_URL", "\"${backendBaseUrl.replace("\"", "\\\"")}\"")

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
    debug {
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  androidResources {
    noCompress += "tflite"
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }

  ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
  }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services)
  implementation(libs.androidx.security.crypto)
  implementation(libs.googleid)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.retrofit)
  implementation(libs.tensorflow.lite)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
