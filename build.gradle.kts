// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.google.gms.google.services) apply false
}

buildscript {
    repositories {
        google() // Repositorio de Google
        mavenCentral() // Repositorio central de Maven

    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.0.0") // Versión de Gradle, ajusta si es necesario
        classpath("com.google.gms:google-services:4.3.15") // Firebase/Google Services
        classpath ("com.google.gms:google-services:4.4.2")  //Cloud firestore
    }
}

allprojects {
    repositories {
        google() // Repositorio de Google
        mavenCentral() // Repositorio central de Maven

    }
}