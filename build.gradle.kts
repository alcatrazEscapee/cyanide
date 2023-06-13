
// From gradle.properties
val modName: String by extra
val modAuthor: String by extra
val modId: String by extra
val modGroup: String by extra
val modSourceUrl: String by extra
val modIssueUrl: String by extra
val modHomeUrl: String by extra
val modDescription: String by extra
val modJavaVersion: String by extra
val modVersion: String = System.getenv("VERSION") ?: "0.0.0-indev"

val minecraftVersion: String by extra
val minecraftVersionRange: String by extra
val forgeVersion: String by extra
val forgeVersionRange: String by extra
val parchmentVersion: String by extra
val parchmentMinecraftVersion: String by extra
val fabricLoaderVersion: String by extra
val epsilonVersion: String by extra


subprojects {

    version = modVersion
    group = modGroup

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(JavaLanguageVersion.of(modJavaVersion).asInt())
    }

    tasks.withType<Jar> {
        manifest {
            attributes(mapOf(
                "Implementation-Title" to modName,
                "Implementation-Version" to modVersion,
                "Implementation-Vendor" to modAuthor,
            ))
        }
    }

    // Apply properties from gradle.properties to mod specific files, meaning they don't need to be changed on version update.
    tasks.withType<ProcessResources> {
        filesMatching(listOf("META-INF/mods.toml", "pack.mcmeta", "fabric.mod.json")) {
            expand(mapOf(
                "modName" to modName,
                "modAuthor" to modAuthor,
                "modId" to modId,
                "modGroup" to modGroup,
                "modSourceUrl" to modSourceUrl,
                "modIssueUrl" to modIssueUrl,
                "modHomeUrl" to modHomeUrl,
                "modDescription" to modDescription,
                "modJavaVersion" to modJavaVersion,
                "modVersion" to modVersion,

                "minecraftVersion" to minecraftVersion,
                "minecraftVersionRange" to minecraftVersionRange,
                "forgeVersion" to forgeVersion,
                "forgeVersionRange" to forgeVersionRange,
                "fabricLoaderVersion" to fabricLoaderVersion,
            ))
        }
    }

    // Disables Gradle's custom module metadata from being published to maven. The metadata includes mapped dependencies which are not reasonably consumable by other mod developers.
    tasks.withType<GenerateModuleMetadata> {
        enabled = false
    }
}

tasks.register<Exec>("makeErrorsAsDataPack") {
    workingDir = file("${projectDir}/tests/errors_as_datapack")
    commandLine = listOf("jar", "-cMf", "../errors_as_datapack.zip", "pack.mcmeta", "data")
}

tasks.register<Exec>("makeFeatureCycleAsDataPack") {
    workingDir = file("${projectDir}/tests/feature_cycle_as_datapack")
    commandLine = listOf("jar", "-cMf", "../feature_cycle_as_datapack.zip", "pack.mcmeta", "data")
}

tasks.register<GradleBuild>("makeAllDataPacks") {
    tasks = listOf("makeErrorsAsDataPack", "makeFeatureCycleAsDataPack")
}
