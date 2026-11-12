import org.gradle.api.tasks.testing.Test

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}

subprojects {
    tasks.withType<Test>().configureEach {
        // Robolectric fetches android-all at test time via its own downloader,
        // ignoring Gradle repository config — Central 429s kill every test JVM.
        systemProperty(
            "robolectric.dependency.repo.url",
            "https://maven-central.storage-download.googleapis.com/maven2/",
        )
    }
}
