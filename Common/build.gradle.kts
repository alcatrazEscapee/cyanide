plugins {
    id("net.neoforged.moddev") version "0.1.112"
}

val parchmentMinecraftVersion: String by extra
val parchmentVersion: String by extra
val modNeoFormVersion: String by extra

dependencies {
    compileOnly(group = "org.spongepowered", name = "mixin", version = "0.8.5")
    compileOnly(group = "io.github.llamalad7", name = "mixinextras-common", version = "0.3.5")
    annotationProcessor(group = "io.github.llamalad7", name = "mixinextras-common", version = "0.3.5")
    annotationProcessor(group = "systems.manifold", name = "manifold-preprocessor", version = "2024.1.34-20241001.011431-2")
}

neoForge {
    neoFormVersion.set(modNeoFormVersion)

    parchment {
        minecraftVersion.set(parchmentMinecraftVersion)
        mappingsVersion.set(parchmentVersion)
    }
}
