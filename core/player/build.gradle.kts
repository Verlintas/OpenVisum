plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "verlintas.openvisum.core.player"
    compileSdk = 37

    defaultConfig {
        minSdk = 34
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}


dependencies {
    api(project(":core:common"))
    api(libs.libvlc)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
