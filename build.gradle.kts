import net.fabricmc.loom.task.RemapJarTask
import org.gradle.api.provider.Provider

plugins {
    `maven-publish`
    id("fabric-loom") version "1.11-SNAPSHOT"
    id("com.modrinth.minotaur") version "2.+"
}

val modId = property("slug").toString()
val version = "${property("slug")}+${property("baseVersion")}+${property("deps.minecraft")}"

stonecutter {
    val loader = property("deps.loader").toString()
    constants.match(
        loader, "fabric", "neoforge"
    )
}

repositories {
    maven("https://repo.sleeping.town/")
    maven("https://maven.terraformersmc.com/")
    maven("https://maven.shedaniel.me/")
    maven("https://api.modrinth.com/maven")
    maven("https://maven.parchmentmc.org")
    maven("https://modmaven.k-4u.nl/")
    maven("https://jm.gserv.me/repository/maven-public/")
    maven("https://cursemaven.com")
    maven("https://maven.su5ed.dev/releases")
    maven("https://maven.neoforged.net/releases/")
    mavenCentral()
}

dependencies {
    println("Project version: $version")
    println("Minecraft Version: ${stonecutter.current.version}")
    println("Mod Loader: ${property("deps.loader")}")

    minecraft("com.mojang:minecraft:${property("deps.minecraft")}")
    mappings(loom.layered() {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-${property("deps.minecraft")}:${property("deps.parchment")}@zip")
    })

    if (property("deps.loader") == "fabric") {
        modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
        modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")

        modImplementation("folk.sisby:surveyor:${property("deps.surveyor")}")
        include("folk.sisby:surveyor:${property("deps.surveyor")}")
    }

    if (property("deps.loader") == "neoforge") {
        modImplementation("net.neoforged:neoforge:${property("deps.neoforge_version")}")
        modImplementation("org.sinytra.forgified-fabric-api:forgified-fabric-api:${property("deps.fabric_api")}")
    }

    // modCompileOnly("maven.modrinth:iris:${property("deps.iris")}")
    compileOnly("net.fabricmc:sponge-mixin:0.12.5+mixin.0.8.5")
    annotationProcessor("net.fabricmc:sponge-mixin:0.12.5+mixin.0.8.5")

    modImplementation("mysticdrew:common-networking-common:${property("deps.commonnetworking")}")
    modImplementation("maven.modrinth:data-anchor:${property("deps.data-anchor")}")
    modImplementation("maven.modrinth:midnightlib:${property("deps.midnightlib")}")
    modImplementation("curse.maven:selene-499980:${property("deps.moonlightlib")}")

    include("mysticdrew:common-networking-common:${property("deps.commonnetworking")}")
    include("maven.modrinth:data-anchor:${property("deps.data-anchor")}")
    include("maven.modrinth:midnightlib:${property("deps.midnightlib")}")
    include("curse.maven:selene-499980:${property("deps.moonlightlib")}")

    modCompileOnly("maven.modrinth:supplementaries:${property("deps.supplementaries")}")

    modImplementation("com.google.code.gson:gson:2.10.1")

}

loom {
    // accessWidenerPath.set(file("src/main/resources/$modId.accesswidener"))

    mixin {
        defaultRefmapName = "$modId.refmap.json"
    }

    // runConfigs {
    //     create("client") {
    //         vmArg("-Dsodium.checks.issue2561=false")
    //         programArg("--username=Dev")
    //         runDir("run/client")
    //     }
    //     create("server") {
    //         runDir("run/server")
    //     }
    // }
}

tasks.processResources {
    inputs.property("minecraft", stonecutter.current.version)
    inputs.property("loader", project.property("deps.fabric_loader"))
    inputs.property("api", project.property("deps.fabric_api"))

    filesMatching("fabric.mod.json") { expand(mapOf(
        "mc" to stonecutter.current.version,
        "modId" to rootProject.property("slug"),
        "version" to "${rootProject.property("slug")}+${rootProject.property("baseVersion")}+${stonecutter.current.version}",
        "modName" to rootProject.property("modName"),
        "modDescription" to rootProject.property("modDescription"),
        "authors" to rootProject.property("authors"),
        "contributors" to rootProject.property("contributors"),
        "homepage" to rootProject.property("homepage"),
        "issues" to rootProject.property("issues"),
        "sources" to rootProject.property("sources"),
        "license" to rootProject.property("license"),
        "fl" to project.property("deps.fabric_loader"),
        "fapi" to project.property("deps.fabric_api")
    )) }
}

// tasks.remapJar {
//     dependsOn(tasks.jar)
//     inputJar.set(tasks.jar.get().archiveFile)
// }

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 21
    options.compilerArgs.add("-Xlint:none")
}

java {
    withSourcesJar()
    val java = if (stonecutter.eval(stonecutter.current.version, ">=1.20.5"))
        JavaVersion.VERSION_21 else JavaVersion.VERSION_17
    targetCompatibility = java
    sourceCompatibility = java
}

sourceSets {
    main {
        resources {
            srcDirs.add(file("src/main/generated"))
        }
    }
}

// modrinth {
//     token = System.getenv("MODRINTH_TOKEN")
//     projectId = property("modrinthSlug")
//     versionNumber = project.version.toString()
//     uploadFile = remapJar
//     gameVersions = property("compatibleVersions").split(", ").toList()
//     loaders = property("compatibleLoaders").split(", ").toList()
//     changelog = if (!file("CHANGELOG.md").exists()) "" else rootProject.file("CHANGELOG.md").readText()
//     syncBodyFrom = rootProject.file("README.md").readText()
//     dependencies {
//         required.version("net.fabricmc.fabric-api", fapiVersion)
//         required.version("folk.sisby:surveyor", surveyorVersion)
//         optional.version("maven.modrinth:supplementaries", supplementariesVersion)
//     }
// }