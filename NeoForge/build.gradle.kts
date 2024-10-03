plugins {
    id("net.neoforged.moddev") version "0.1.112"
}

val modId: String by extra
val modGroup: String by extra
val minecraftVersion: String by extra
val neoForgeVersion: String by extra
val parchmentVersion: String by extra
val parchmentMinecraftVersion: String by extra

base {
    archivesName.set("${modId}-neoforge-${minecraftVersion}")
}

dependencies {
    implementation(project(":Common"))
}

neoForge {
    version.set(neoForgeVersion)

    addModdingDependenciesTo(sourceSets.test.get())

    parchment {
        minecraftVersion.set(parchmentMinecraftVersion)
        mappingsVersion.set(parchmentVersion)
    }

    runs {
        configureEach {
            jvmArgument("-XX:+AllowEnhancedClassRedefinition")
        }
        register("client") { client() }
        register("server") { server() }
    }

    mods {
        create(modId) {
            sourceSet(sourceSets.main.get())
            sourceSet(sourceSets.test.get())
        }
    }
}

tasks {
    named<JavaCompile>("compileJava") { source(project(":Common").sourceSets.main.get().allSource) }
    named<ProcessResources>("processResources") { from(project(":Common").sourceSets.main.get().resources) }
}