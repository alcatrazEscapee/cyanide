plugins {
    java
    id("org.spongepowered.gradle.vanilla") version "0.2.1-SNAPSHOT"
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
