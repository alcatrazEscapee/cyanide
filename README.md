## Cyanide

A dev-time (`runtimeOnly`) dependency for Minecraft mods, to fix those annoying little things that you definitely don't want to be shipping in your mod because they're awful gross hacks.

Among those:
- Adding or changing vanilla log message for better debugging.
  - Remove the stacktrace from invalid loot table and recipe errors
  - Add an error message for world gen datapack errors identifying the original file(s) that errored.
- Adding injections for running client self tests, in the same location as Vanilla's `Minecraft#selfTest`.
- Disabling data fixer loading. (This is a performance mod now.)

Usage:

```groovy
repositories {
    maven {
        name 'Jitpack'
        url 'https://jitpack.io'
    }
}

dependencies {
    runtimeOnly fg.deobf("com.github.alcatrazEscapee:cyanide:${cyanide_version}") { transitive = false }
}

// Enabling specific cyanide features
minecraft {
    runs {
        client {
            // Method must be a public static void with no arguments, in a public class. Will be invoked reflectively. Can throw exceptions.
            property 'cyanide.client_self_test', 'package.FullyQualifiedClassName#methodToRunSelfTests'
            // Enables things that may break other things (removing DFU)
            property 'cyanide.enable_dangerous_features', 'true'
        }
    }
}
```