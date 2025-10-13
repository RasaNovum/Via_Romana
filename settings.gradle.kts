pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.kikugie.dev/releases")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
    id("dev.kikugie.stonecutter") version "0.7.10"
}

rootProject.name = "via-romana"

stonecutter {
    create(rootProject) {
        fun match(version: String, vararg loaders: String) = loaders
            .forEach { loader -> vers("$version-$loader", version).buildscript = "build.$loader.gradle.kts" }

        match("1.21.1", "fabric", "neoforge")
        match("1.20.1", "fabric")

        vcsVersion = "1.20.1-fabric"
    }
}

