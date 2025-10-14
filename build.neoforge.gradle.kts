import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    id("net.neoforged.moddev")
}

version = "${property("mod.version")}+${property("deps.minecraft")}-neoforge"
base.archivesName = property("mod.id") as String

neoForge {
    version = property("deps.neoforge_version") as String

    if (hasProperty("deps.parchment")) {
        parchment {
            minecraftVersion = property("deps.minecraft") as String
            mappingsVersion = property("deps.parchment") as String
        }
    }

    runs {
        register("client") {
            client()
        }
        register("server") {
            server()
        }
        register("data") {
            data()
        }
    }

    mods {
        register(property("mod.id") as String) {
            sourceSet(sourceSets.main.get())
        }
    }
}

repositories {
    maven("https://maven.su5ed.dev/releases")
    maven("https://repo.sleeping.town/")
    maven("https://maven.terraformersmc.com/")
    maven("https://maven.shedaniel.me/")
    maven("https://api.modrinth.com/maven")
    maven("https://maven.parchmentmc.org")
    maven("https://modmaven.k-4u.nl/")
    maven("https://jm.gserv.me/repository/maven-public/")
    maven("https://cursemaven.com")
    maven("https://maven.sinytra.org/releases")
    mavenCentral()
}

dependencies {
//    implementation("org.sinytra.forgified-fabric-api:forgified-fabric-api:${property("deps.fabric_api")}")

    implementation("mysticdrew:common-networking-common:${property("deps.commonnetworking")}")
    implementation("maven.modrinth:data-anchor:${property("deps.data-anchor")}")
    implementation("maven.modrinth:midnightlib:${property("deps.midnightlib")}")
    implementation("curse.maven:selene-499980:${property("deps.moonlightlib")}")
    implementation("com.google.code.gson:gson:2.10.1")

    implementation("maven.modrinth:supplementaries:${property("deps.supplementaries")}")

    annotationProcessor("net.fabricmc:sponge-mixin:0.12.5+mixin.0.8.5")
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
        "neoforge" to project.property("deps.neoforge_version"),
        "forgeFapi" to project.property("deps.fabric_api")
    )

    inputs.properties(props)

    filesMatching("META-INF/neoforge.mods.toml") {
        expand(props)
    }

    // Exclude Fabric-specific files
    exclude("**/fabric.mod.json", "**/*.accesswidener", "**/forge.mods.toml")
}


tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    val javaVersion = if (stonecutter.eval(stonecutter.current.version, ">=1.20.5")) 21 else 17
    options.release.set(javaVersion)

    options.compilerArgs.addAll(listOf(
        "-AoutRefMapFile=${project.buildDir}/resources/main/${project.property("mod.id")}.refmap.json",
        "-AdefaultObfuscationEnv=searge"
    ))
}

java {
    withSourcesJar()
    val javaVersion = if (stonecutter.eval(stonecutter.current.version, ">=1.20.5")) 21 else 17
    sourceCompatibility = JavaVersion.toVersion(javaVersion)
    targetCompatibility = JavaVersion.toVersion(javaVersion)
}