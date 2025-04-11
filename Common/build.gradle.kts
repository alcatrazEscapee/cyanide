plugins {
    id("net.neoforged.moddev") version "2.0.78"
}

val parchmentMinecraftVersion: String by extra
val parchmentVersion: String by extra
val modNeoFormVersion: String by extra
val modManifoldVersion: String by extra

dependencies {
    compileOnly(group = "org.spongepowered", name = "mixin", version = "0.8.7")
    compileOnly(group = "io.github.llamalad7", name = "mixinextras-common", version = "0.4.1")
    annotationProcessor(group = "io.github.llamalad7", name = "mixinextras-common", version = "0.4.1")
    annotationProcessor(group = "systems.manifold", name = "manifold-preprocessor", version = modManifoldVersion)
}

neoForge {
    neoFormVersion = modNeoFormVersion

    parchment {
        minecraftVersion.set(parchmentMinecraftVersion)
        mappingsVersion.set(parchmentVersion)
    }
}
