# Cyanide

---

*cyanide seems to be something that will make me want to drink cyanide a lot less - Starmute, world generation datapack wizard.*

For [**Forge**](https://www.curseforge.com/minecraft/mc-mods/cyanide-forge) or [**Fabric**](https://www.curseforge.com/minecraft/mc-mods/cyanide-fabric).

A mod which substantially improves Minecraft's data driven world generation error detection and recovery mechanisms. For example, consider the following fairly common error in Vanilla:

```
Feature: Not a JSON object: "minecraft:disk_gravel"; Not a JSON object: "minecraft:disk_clay"; Not a JSON object: "minecraft:disk_sand"; Not a JSON object: "minecraft:ore_copper"; Not a JSON object: "minecraft:ore_lapis"; Not a JSON object: "minecraft:ore_diamond"; Not a JSON object: "minecraft:ore_redstone"; Not a JSON object: "minecraft:ore_gold"; Not a JSON object: "minecraft:ore_iron"; Not a JSON object: "minecraft:ore_coal"; Not a JSON object: "cyanide:broken_disk_gravel"; Not a JSON object: "minecraft:ore_deepslate"; Not a JSON object: "minecraft:ore_tuff"; Not a JSON object: "minecraft:ore_andesite"; Not a JSON object: "minecraft:ore_diorite"; Not a JSON object: "minecraft:ore_granite"; Not a JSON object: "minecraft:ore_gravel"; Not a JSON object: "minecraft:ore_dirt"
[19:04:44] [Render thread/WARN]: Failed to validate datapack
java.util.concurrent.CompletionException: com.google.gson.JsonParseException: Error loading registry data: No key snowy in MapLike[{"not_snowy":"true"}]
```

And now compare the error, using the exact same datapack, with Cyanide included:

```
Error(s) loading registry minecraft:worldgen/biome:
No key snowy in MapLike[{"not_snowy":"true"}]
	at: file "data/cyanide/worldgen/configured_feature/broken_disk_gravel.json"
	at: data pack cyanide-debug.zip
	at: reference to "cyanide:broken_disk_gravel" from minecraft:worldgen/configured_feature
Missing feature at index 7
	at: "features", step underground_ores, index 6
	at: file "data/cyanide/worldgen/biome/plains_broken_feature.json"
	at: data pack cyanide-debug.zip
Missing feature at index 6
	at: "features", step underground_ores, index 6
	at: file "data/cyanide/worldgen/biome/plains_unregistered_feature.json"
	at: data pack cyanide-debug.zip; 

Error(s) loading registry minecraft:worldgen/configured_feature:
No key snowy in MapLike[{"not_snowy":"true"}]
	at: file "data/cyanide/worldgen/configured_feature/broken_disk_gravel.json"
	at: data pack cyanide-debug.zip
Unknown element name: invalid_heightmap
	at: file "data/cyanide/worldgen/configured_feature/broken_disk_gravel_2.json"
	at: data pack cyanide-debug.zip
```

### Features

Below is a (hopefully, comprehensive) list of all changes, tweaks, and improvements Cyanide makes to vanilla datapack error messages, including locations where the format is narrowed from vanilla in name of clarity.

- Append the file to each error message. (`at: file "data/cyanide/blah/blah.json"`)
- Append the source datapack to each error message (`at: data pack blah.zip`)
- Group and categorize errors by registry.
- Several improvements for error recovery:
  - If an error occurs during a single registry parsing, don't abort parsing others.
  - Record all errors that occur parsing a single registry, not just the first one.
- Optional fields, in some cases, will error if a value is present, but invalid. Example:

```
In a biome json, the following is a valid temperature modifier.
It is the same as using a temperature modifier of 'none'.
In Cyanide, this would error.

"temperature_modifier": "hocus pocus!"
```

- Improvements for select codecs, which now report trace information to identify where in the file an error occurred.
  - Features in biomes identify both the index in the list, and the index as a generation step.
  - Other fields such as `carvers`, `surface_builder`, etc. in biomes will be traced in error messages.
- Removed the `Feature: Not a JSON Object` log messages. These are now upgraded to full errors.
- Some fields, such as biome categories, temperature modifiers, and precipitations, have slightly improved error messages and will also show the range of valid values. Example:
  
```
Unknown category name: not_a_category, expected one of [none, taiga, extreme_hills, jungle, mesa, plains, savanna, icy, the_end, beach, forest, ocean, desert, river, swamp, mushroom, nether, underground]
	at: file "data/cyanide/worldgen/biome/invalid_category.json"
	at: data pack test_data_pack.zip
```

- Template pools have additional error reporting identifying where in the file errors had occurred.
- Invalid processor lists in template pools will be caught at datapack load instead of at runtime, causing hard crashes.
- Invalid registry entries will error at datapack load rather than either defaulting, or returning null at runtime.
- Feature cycle errors are improved, and will now print the exact features, indices, and source biomes (all by registry name) in the cycle, in order of the cycle.
- Improvements for log messages for invalid loot tables and recipes:
  - Stack traces are not printed to the log.
  - Offending file names are identified and printed instead.
- The "Loaded X Recipe(s)" log message is now accurate.

### Using

To include Cyanide in your development environment (Forge):

```groovy
repositories {
    maven { url = 'https://alcatrazescapee.jfrog.io/artifactory/mods' }
}

dependencies {
    runtimeOnly fg.deobf("com.alcatrazescapee:cyanide-forge-1.18:VERSION") { transitive = false }
}
```