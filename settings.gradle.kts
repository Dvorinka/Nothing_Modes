pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories { google(); mavenCentral() }
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
