pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        // Google's read-only Maven Central mirror — repo.maven.apache.org rate-limits the CI fleet (429).
        maven { url = uri("https://maven-central.storage-download.googleapis.com/maven2/") }
        mavenCentral()
    }
}

rootProject.name = "NothingModes"
include("engine-core")
include("core-shizuku")
include("device-tools")
include("capabilities")
include("nothing-integrations")
include("data")
include("automation-android")
include("ui")
include("app")
