import net.fabricmc.loom.task.RemapJarTask
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    id("fabric-loom")
    id("com.modrinth.minotaur") version "2.+"
}

version = "${property("mod.version")}+${property("deps.minecraft")}-fabric"
base.archivesName = property("mod.id") as String

loom {
    mixin {
        defaultRefmapName = "${property("mod.id")}.refmap.json"
    }
    accessWidenerPath = rootProject.file("src/main/resources/${property("mod.id")}.accesswidener")
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
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:${property("deps.minecraft")}")
    mappings(loom.layered {
        officialMojangMappings()
        if (hasProperty("deps.parchment")) {
            parchment("org.parchmentmc.data:parchment-${property("deps.minecraft")}:${property("deps.parchment")}@zip")
        }
    })

    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")

    modImplementation("mysticdrew:common-networking-fabric:${property("deps.commonnetworking")}")
    modImplementation("mysticdrew:common-networking-common:${property("deps.commonnetworking")}")
    modImplementation("maven.modrinth:data-anchor:${property("deps.data-anchor")}")
    modImplementation("maven.modrinth:midnightlib:${property("deps.midnightlib")}")
    modImplementation("maven.modrinth:moonlight:${property("deps.moonlightlib")}")

    include("mysticdrew:common-networking-fabric:${property("deps.commonnetworking")}")
    include("maven.modrinth:data-anchor:${property("deps.data-anchor")}")
    include("maven.modrinth:midnightlib:${property("deps.midnightlib")}")
    include("mysticdrew:common-networking-common:${property("deps.commonnetworking")}")
    include("maven.modrinth:moonlight:${property("deps.moonlightlib")}")

    modCompileOnly("maven.modrinth:supplementaries:${property("deps.supplementaries")}")
    modCompileOnly("folk.sisby:surveyor:${property("deps.surveyor")}")

//    modImplementation("folk.sisby:surveyor:${property("deps.surveyor")}")
//    include("folk.sisby:surveyor:${property("deps.surveyor")}")

    annotationProcessor("net.fabricmc:sponge-mixin:0.12.5+mixin.0.8.5")
    modImplementation("com.google.code.gson:gson:2.10.1")
}

tasks.named<ProcessResources>("processResources") {
    val props = mapOf(
        "version" to project.version,
        "mc" to project.property("deps.minecraft"),

        "modName" to project.property("mod.name"),
        "modId" to project.property("mod.id"),
        "modDescription" to project.property("mod.description"),
        "authors" to project.property("mod.authors"),
        "contributors" to project.property("mod.contributors"),
        "license" to project.property("mod.license"),
        "homepage" to project.property("mod.homepage"),
        "issues" to project.property("mod.issues"),
        "sources" to project.property("mod.sources"),

        "fl" to project.property("deps.fabric_loader"),
        "fapi" to project.property("deps.fabric_api")
    )

    inputs.properties(props)

    filesMatching("fabric.mod.json") { expand(props) }

    exclude("**/neoforge.mods.toml", "**/mods.toml")
}

stonecutter {
    val loaderClientField = "@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)"
    val stringReplacements = mapOf(
        "@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)" to loaderClientField,
        "@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)" to loaderClientField
    )

    stringReplacements.forEach { (from, to) ->
        replacements.string {
            direction = true
            replace(from, to)
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    val javaVersion = if (stonecutter.eval(stonecutter.current.version, ">=1.20.5")) 21 else 17
    options.release.set(javaVersion)
}


java {
    withSourcesJar()
    val javaVersion = if (stonecutter.eval(stonecutter.current.version, ">=1.20.5")) 21 else 17
    sourceCompatibility = JavaVersion.toVersion(javaVersion)
    targetCompatibility = JavaVersion.toVersion(javaVersion)
}

//modrinth {
//    token = System.getenv("MODRINTH_TOKEN")
//    projectId = property("publish.modrinth") as String
//    versionNumber = project.version.toString()
//    versionName = "${property("mod.name")} ${project.version} for Fabric ${property("deps.minecraft")}"
//    val remapJarTask = tasks.named<RemapJarTask>("remapJar")
//    uploadFile.set(remapJarTask.flatMap { it.archiveFile })
//    gameVersions.add(property("deps.minecraft") as String)
//    dependencies {
//        required.project("fabric-api")
//    }
//}
