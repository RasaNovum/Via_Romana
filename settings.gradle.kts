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
    id("dev.kikugie.stonecutter") version "0.7.10"
}

rootProject.name = "via-romana"

stonecutter {
    create(rootProject) {
        vers("1.21.1-fabric", "1.21.1")
        vers("1.20.1-fabric", "1.20.1")
        vers("1.21.1-neoforge", "1.21.1")
    }
}