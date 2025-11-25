import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    id("net.minecraftforge.gradle") version ("6.0.46")
    id("org.spongepowered.mixin") version "0.7.+"
    id("org.parchmentmc.librarian.forgegradle") version "1.+"
    id("me.modmuss50.mod-publish-plugin")
}

version = "${property("mod.version")}+${property("deps.minecraft")}-forge"
base.archivesName = property("mod.id") as String

minecraft {
    mappings("parchment", "2023.09.03-1.20.1")

    runs {
        create("client") {
            workingDirectory(project.file("run"))
            property("forge.logging.markers", "REGISTRIES")
            property("forge.logging.console.level", "debug")
            mods {
                create(property("mod.id") as String) {
                    source(sourceSets.main.get())
                }
            }
        }

        create("server") {
            workingDirectory(project.file("run"))
            property("forge.logging.markers", "REGISTRIES")
            property("forge.logging.console.level", "debug")
            mods {
                create(property("mod.id") as String) {
                    source(sourceSets.main.get())
                }
            }
        }

        create("data") {
            workingDirectory(project.file("run"))
            property("forge.logging.markers", "REGISTRIES")
            property("forge.logging.console.level", "debug")
            args("--mod", property("mod.id") as String, "--all", "--output", file("src/generated/resources/"), "--existing", file("src/main/resources/"))
            mods {
                create(property("mod.id") as String) {
                    source(sourceSets.main.get())
                }
            }
        }
    }
}

mixin {
    add(sourceSets.main.get(), "via_romana.refmap.json")
    config("via_romana.mixins.json")
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
    minecraft("net.minecraftforge:forge:${property("deps.minecraft")}-${property("deps.forge_version")}")

    implementation(fg.deobf("mysticdrew:common-networking-common:${property("deps.commonnetworking")}"))
    implementation(fg.deobf("mysticdrew:common-networking-forge:${property("deps.commonnetworking")}"))
    implementation(fg.deobf("maven.modrinth:data-anchor:${property("deps.data-anchor")}"))
    implementation(fg.deobf("maven.modrinth:midnightlib:${property("deps.midnightlib")}"))
    implementation(fg.deobf("maven.modrinth:moonlight:${property("deps.moonlightlib")}"))

    compileOnly(fg.deobf("maven.modrinth:supplementaries:${property("deps.supplementaries")}"))

    implementation("com.google.code.gson:gson:2.10.1")

    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
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
        "forge" to project.property("deps.forge_version"),
        "forgeFapi" to project.property("deps.fabric_api")
    )

    inputs.properties(props)

    filesMatching("META-INF/mods.toml") {
        expand(props)
    }

    exclude("**/fabric.mod.json", "**/*.accesswidener", "**/neoforge.mods.toml")
}

stonecutter {
    val loaderClientField = "@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)"
    val stringReplacements = mapOf(
        "@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)" to loaderClientField,
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
    val javaVersion = 17
    options.release.set(javaVersion)
    
    exclude("**/integration/surveyor/**")
}

java {
    withSourcesJar()
    val javaVersion = 17
    sourceCompatibility = JavaVersion.toVersion(javaVersion)
    targetCompatibility = JavaVersion.toVersion(javaVersion)
}

publishMods {
    file = tasks.jar.get().archiveFile
    changelog = rootProject.file("CHANGELOG.md").takeIf { it.exists() }?.readText() ?: "No changelog provided"
    type = STABLE
    modLoaders.addAll("forge", "neoforge")
    
    modrinth {
        accessToken = providers.environmentVariable("MODRINTH_TOKEN")
        projectId = property("publish.modrinth") as String
        minecraftVersions.add(property("deps.minecraft") as String)

        requires { slug = "fabric-api" }
        requires { slug = "common-network" }
        requires { slug = "data-anchor" }
        requires { slug = "midnightlib" }
        requires { slug = "moonlight" }
    }
    
    curseforge {
        accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
        projectId = property("publish.curseforge") as String
        minecraftVersions.add(property("deps.minecraft") as String)

        clientRequired = true
        serverRequired = true

        requires { slug = "fabric-api" }
        requires { slug = "common-network" }
        requires { slug = "data-anchor" }
        requires { slug = "midnightlib" }
        requires { slug = "selene" }
    }
}
