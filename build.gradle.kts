val modName: String by extra
val modAuthor: String by extra
val modId: String by extra
val modGroup: String by extra
val modIssueUrl: String by extra
val modHomeUrl: String by extra
val modSourceUrl: String by extra
val modDescription: String by extra
val modJavaVersion: String by extra
val modVersion: String = System.getenv("VERSION") ?: "0.0.0-indev"

val minecraftVersion: String by extra
val minecraftVersionRange: String by extra
val neoForgeVersion: String by extra
val neoForgeVersionRange: String by extra
val parchmentVersion: String by extra
val fabricVersion: String by extra
val fabricLoaderVersion: String by extra

plugins {
    id("org.jetbrains.gradle.plugin.idea-ext") version "1.1.7"
}

subprojects {
    version = modVersion
    group = modGroup

    repositories {
        exclusiveContent {
            forRepository { maven("https://maven.parchmentmc.org/") }
            filter { includeGroup("org.parchmentmc.data") }
        }
        exclusiveContent {
            forRepository { maven("https://alcatrazescapee.com/maven") }
            filter { includeGroup("com.alcatrazescapee") }
        }
        exclusiveContent {
            forRepository { maven("https://cursemaven.com") }
            filter { includeGroup("curse.maven") }
        }
    }

    tasks {
        withType<ProcessResources> {
            filesMatching(listOf("META-INF/neoforge.mods.toml", "pack.mcmeta", "fabric.mod.json")) {
                expand(mapOf(
                    "modName" to modName,
                    "modAuthor" to modAuthor,
                    "modId" to modId,
                    "modGroup" to modGroup,
                    "modIssueUrl" to modIssueUrl,
                    "modHomeUrl" to modHomeUrl,
                    "modSourceUrl" to modSourceUrl,
                    "modDescription" to modDescription,
                    "modJavaVersion" to modJavaVersion,
                    "modVersion" to modVersion,

                    "minecraftVersion" to minecraftVersion,
                    "minecraftVersionRange" to minecraftVersionRange,
                    "neoForgeVersion" to neoForgeVersion,
                    "neoForgeVersionRange" to neoForgeVersionRange,
                    "fabricVersion" to fabricVersion,
                    "fabricLoaderVersion" to fabricLoaderVersion,
                ))
            }
        }
    }
}