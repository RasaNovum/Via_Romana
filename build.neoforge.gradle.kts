import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    id("net.neoforged.moddev")
    id("me.modmuss50.mod-publish-plugin")
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
    implementation("mysticdrew:common-networking-common:${property("deps.commonnetworking")}")
    implementation("mysticdrew:common-networking-neoforge:${property("deps.commonnetworking")}")
    implementation("maven.modrinth:data-anchor:${property("deps.data-anchor")}")
    implementation("maven.modrinth:midnightlib:${property("deps.midnightlib")}")
    implementation("maven.modrinth:moonlight:${property("deps.moonlightlib")}")

    compileOnly("maven.modrinth:supplementaries:${property("deps.supplementaries")}")

    implementation("com.google.code.gson:gson:2.10.1")
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

    exclude("**/fabric.mod.json", "**/*.accesswidener", "**/mods.toml")
}

stonecutter {
    val loaderClientField = "@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)"
    val stringReplacements = mapOf(
        "@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)" to loaderClientField,
        "@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)" to loaderClientField
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
    
    exclude("**/integration/surveyor/**")
}

java {
    withSourcesJar()
    val javaVersion = if (stonecutter.eval(stonecutter.current.version, ">=1.20.5")) 21 else 17
    sourceCompatibility = JavaVersion.toVersion(javaVersion)
    targetCompatibility = JavaVersion.toVersion(javaVersion)
}

publishMods {
    file = tasks.jar.get().archiveFile
    changelog = rootProject.file("CHANGELOG.md").takeIf { it.exists() }?.readText() ?: "No changelog provided"
    type = STABLE
    modLoaders.add("neoforge")
    
    modrinth {
        accessToken = providers.environmentVariable("MODRINTH_TOKEN")
        projectId = property("publish.modrinth") as String
        minecraftVersions.add(property("deps.minecraft") as String)
        
        requires {
            slug = "common-network"
        }
        requires {
            slug = "data-anchor"
        }
        requires {
            slug = "midnightlib"
        }
        requires {
            slug = "moonlight"
        }
    }
    
    curseforge {
        accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
        projectId = property("publish.curseforge") as String
        minecraftVersions.add(property("deps.minecraft") as String)
        
        requires {
            slug = "common-network"
        }
        requires {
            slug = "data-anchor"
        }
        requires {
            slug = "midnightlib"
        }
        requires {
            slug = "selene"
        }
    }
}