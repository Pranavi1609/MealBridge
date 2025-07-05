pluginManagement {
    repositories {
        google()  // Google's Maven repository
        mavenCentral()  // Maven Central repository
        gradlePluginPortal()  // Gradle Plugin Portal
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)  // Ensures repositories are set in settings.gradle.kts
    repositories {
        google() {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "Meal Bridge"
include(":app")