// Later versions of vanillagradle are broken without newer versions of gradle
// This is pinned to a version that works, since for the rest of 1.20.1 lifecycle, we're not expecting to update this, or gradle
// In 1.21 this will move to MDG instead
buildscript {
    dependencies.add("classpath", "org.spongepowered:vanillagradle:0.2.1-20230929.005621-65")
}

plugins {
    java
    id("org.spongepowered.gradle.vanilla") version "0.2.1-20230929.005621-65"
}

// From gradle.properties
val minecraftVersion: String by extra

minecraft {
    version(minecraftVersion)
}

dependencies {
    compileOnly(group = "org.spongepowered", name = "mixin", version = "0.8.5")

    testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-api", version = "5.8.2")
    testRuntimeOnly(group = "org.junit.jupiter", name = "junit-jupiter-engine", version = "5.8.2")
}

tasks {
    test {
        useJUnitPlatform()
    }
}
