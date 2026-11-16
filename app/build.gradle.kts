import java.util.Base64

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.hollowshote.hollowconvert"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.hollowshote.hollowconvert"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation(platform("androidx.compose:compose-bom:2025.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
}

// Decode the checked-in launcher artwork into a generated Android drawable.
// Keeping the source as Base64 lets the GitHub contents API store it losslessly.
val launcherIconResDir = layout.buildDirectory.dir("generated/res/launcherIcon")
val generateLauncherIcon by tasks.registering {
    val encodedIcon = layout.projectDirectory.file("src/main/assets/ic_launcher_image.png.b64")
    val generatedPng = launcherIconResDir.map { it.file("drawable-nodpi/ic_launcher_image.png") }
    inputs.file(encodedIcon)
    outputs.file(generatedPng)
    doLast {
        val outputFile = generatedPng.get().asFile
        outputFile.parentFile.mkdirs()
        outputFile.writeBytes(Base64.getDecoder().decode(encodedIcon.asFile.readText().trim()))
    }
}

android.sourceSets.getByName("main").res.srcDir(launcherIconResDir)
tasks.named("preBuild").configure {
    dependsOn(generateLauncherIcon)
}
