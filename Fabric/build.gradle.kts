plugins {
    id("fabric-loom") version "1.10.5"
}

val modId: String by extra
val modName: String by extra
val modGroup: String by extra
val modManifoldVersion: String by extra
val minecraftVersion: String by extra
val parchmentVersion: String by extra
val parchmentMinecraftVersion: String by extra
val fabricVersion: String by extra
val fabricLoaderVersion: String by extra

base {
    archivesName.set("${modId}-fabric-${minecraftVersion}")
}

dependencies {
    minecraft(group = "com.mojang", name = "minecraft", version = minecraftVersion)

    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-${parchmentMinecraftVersion}:${parchmentVersion}@zip")
    })

    modImplementation(group = "net.fabricmc", name = "fabric-loader", version = fabricLoaderVersion)
    modImplementation(group = "net.fabricmc.fabric-api", name = "fabric-api", version = fabricVersion)

    implementation(group = "com.google.code.findbugs", name = "jsr305", version = "3.0.2")
    implementation(group = "org.jetbrains", name = "annotations", version = "26.0.2")

    annotationProcessor(group = "systems.manifold", name = "manifold-preprocessor", version = modManifoldVersion)

    compileOnly(project(":Common"))
}

loom {
    mixin {
        defaultRefmapName.set("${modId}.refmap.json")
    }

    runs {
        configureEach {
            ideConfigGenerated(true)
            runDir("run")
            vmArgs("-XX:+AllowEnhancedClassRedefinition")
        }
        named("client") { client() }
        named("server") { server() }
    }
}

tasks {
    named<JavaCompile>("compileJava") {
        source(project(":Common").sourceSets.main.get().allSource)
        options.compilerArgs.add("-APLATFORM_FABRIC")
    }
    named<ProcessResources>("processResources") {
        from(project(":Common").sourceSets.main.get().resources)
    }
}