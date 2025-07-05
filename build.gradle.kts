buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.7.3") // Use stable AGP version
        classpath("com.google.gms:google-services:4.4.2") // Firebase plugin
    }
}
