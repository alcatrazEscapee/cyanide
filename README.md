# Cyanide

---

*cyanide seems to be something that will make me want to drink cyanide a lot less - Starmute, world generation datapack wizard.*

A mod which substantially improves Minecraft's data driven world generation error detection and recovery mechanisms. It removes unnecessary stack traces and generates user-friendly error messages for most common issues with world generation datapacks. For some examples of what improvements Cyanide makes to error reporting, see the below table:

Explanation | Example
-- | --
Errors will have the file, and source datapack appended. Some errors may have additional context if it can be inferred. | <pre>Parsing Error: Value provider too low: 0 [-1--1]<br>&emsp;  at 'placement'<br>&emsp;  at 'cyanide:worldgen/placed_feature/ore_tin' defined in 'file/Test.zip'</pre>
The "Unbound values in registry" error tracks what files were referencing it. | <pre>Missing File Error: 'cyanide:worldgen/configured_feature/big_ores'<br>was referenced but not defined<br>&emsp;  at 'cyanide:worldgen/placed_feature/big_ores' defined in 'file/Test.zip'</pre>
Illegal JSON will show the exact file location, along with the surrounding context of where it failed to parse. | <pre>Syntax Error: Expected ':' at line 3 column 13 path $.config<br>&emsp;  at:<br>{<br>&emsp;  "type": "minecraft:big_flowers",<br>&emsp;  "config" {<br>&emsp;           ^<br>&emsp;           here<br><br>&emsp;  at 'cyanide:worldgen/configured_feature/flowers' defined in  'file/Test.zip'</pre>
"Feature Cycle" errors (where features are defined in different order within different biomes) trace and report the exact cycle found | <pre>A feature cycle was found.<br><br>Cycle:<br>At step 0<br>Feature 'minecraft:lake_lava_underground'<br>&emsp;  must be before 'minecraft:lake_lava_surface' (defined in 'minecraft:ocean'<br>&emsp;    at index 1, 2 and 1 others)<br>&emsp;  must be before 'cyanide:big_ore' (defined in 'minecraft:ocean' at index 2, 3)<br>&emsp;  must be before 'cyanide:small_ore' (defined in 'minecraft:plains' at index 0, 1)<br>&emsp;  must be before 'minecraft:lake_lava_underground' (defined in 'minecraft:ocean'<br>&emsp;    at index 0, 1 and 1 others)</pre>
