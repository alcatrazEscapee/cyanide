## Cyanide

A dev-time (`runtimeOnly`) dependency for Minecraft mods, to fix those annoying little things that you definitely don't want to be shipping in your mod because they're awful gross hacks.

Among those:
- Adding or changing vanilla log message for better debugging.
  - Remove the stacktrace from invalid loot table and recipe errors
  - Add a error message 
- Adding injections to, or running self tests.
- Disabling data fixer loading. (This is a performance mod now.)