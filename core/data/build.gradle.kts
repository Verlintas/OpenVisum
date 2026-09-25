plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "verlintas.openvisum.core.data"
    compileSdk = 37

    defaultConfig {
        minSdk = 34
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}


ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    api(project(":core:common"))
    api(libs.androidx.room.runtime)
    api(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.documentfile)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.jcifs)
    implementation(libs.jupnp)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
