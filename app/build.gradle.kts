plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.jimenez_lozano_ruben_imdbapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.jimenez_lozano_ruben_imdbapp"
        minSdk = 28
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        viewBinding = true
    }

    // Sección de packaging para excluir conflictos en META-INF/INDEX.LIST
    packaging {
        resources {
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/io.netty.versions.properties"
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.play.services.base)
    implementation(libs.firebase.auth)
    implementation(libs.activity)
    implementation(libs.play.services.auth)
    implementation(libs.glide)
    implementation(libs.async.http.client)
    implementation (libs.retrofit)
    implementation (libs.converter.gson)
    implementation ("com.squareup.okhttp3:okhttp:4.9.3")
    implementation ("com.facebook.android:facebook-android-sdk:[12.0.0,13.0.0)")
    implementation ("com.facebook.android:facebook-login:12.3.0,13.0.0")
    implementation ("com.facebook.android:facebook-login:[8,9)")
    implementation(libs.recyclerview)
    implementation(libs.firebase.firestore)
    annotationProcessor(libs.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation ("com.squareup.picasso:picasso:2.71828")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation ("com.google.android.gms:play-services-maps:17.0.1")
    implementation ("com.google.android.libraries.places:places:2.6.0")
    implementation ("com.hbb20:ccp:2.5.0")

}

