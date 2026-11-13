import java.util.Properties
import org.gradle.api.tasks.testing.Test

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val recommendations = Properties().apply {
    val file = rootProject.file("recommendations.properties")
    if (file.isFile) {
        file.inputStream().use { load(it) }
    }
}

fun tmdbApiKey(): String {
    return recommendations.getProperty("tmdb.api.key").orEmpty()
        .trim()
        .substringBefore('\n')
        .substringBefore('\r')
        .replace("\\", "")
        .replace("\"", "")
}

android {
    namespace = "dev.enzotraini.tvlauncher"
    compileSdk = 34
    buildToolsVersion = "34.0.0"

    defaultConfig {
        applicationId = "dev.enzotraini.tvlauncher"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "0.2.0"
        buildConfigField("String", "TMDB_API_KEY", "\"${tmdbApiKey()}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.palette:palette:1.0.0")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}

// O android.jar de teste é um stub de org.json. A implementação real precisa vir antes.
tasks.withType<Test>().configureEach {
    val androidJar = classpath.files.firstOrNull { it.name == "android.jar" } ?: return@configureEach
    classpath = classpath.filter { it.absolutePath != androidJar.absolutePath } + files(androidJar)
}
