# Via Romana 2.2.0 Changelog:

### Features:
- Added `Linked Sign Particles` which indicate which signs are linked to the path network (no more clicking every sign you see in a server).
  - Added `Enable Linked Sign Particles` config option allowing this to be disabled (client-side).
- Added `Custom Cursor` which appears in Via Romana GUI screens as well as when hovering over a linked sign to better indicate left-clicking as the action required to access the `Travel Map` screen.
  - Added `Enable Custom Cursor` config option allowing this to be disabled (client-side).
- Added the ability to `sneak & right-click` anywhere along a charted path to open the `Travel Map` screen using the `Charting Map` item.
  - Added `Enable Remote Map Access` config option allowing users to disable this feature.
- Added movement speed boost while walking on charted paths
  - Added `Path Movement Speedup` config option allowing users to customize (or disable) the speedup.
  - This approach uses proximity to path nodes which avoids issues traditional speedup mods have that work by targeting walked-on blocks which would apply to non-path scenarios like inside buildings.
- Added ability to customize `Travel Map` path line's colours (normal and underground) and opacity via config.
- Added player stats `Distance Walked on Charted Path` and `Distance of Paths Charted`
  - These stats use the vanilla statistics system, so can be viewed in the statistics window or via scoreboard commands. 
  - Added `/viaromana stats get <player> <stat>` and `/viaromana stats set <player> <stat> <value>` for getting and setting of stats.
- Added advancements for each of the above two player stats (each granted at 5 kilometers).

### Improvements:
- Ported networking from `Common Network` to `Data Anchor` to remove a dependency
  - New packet registration system roughly halves the amount of code per-packet making maintenance much easier than before.
- Added `BOOK_PAGE_TURN` sound effect to map screen opening.
- Added teleportation destination randomness (±1 block in XZ & ±0.4 within a block) to prevent groups of people from stacking while still preventing clipping into neighboring blocks.
- Added a `tutorial toast` on first hover of a linked sign for a better new player experience.
- Performance improvements regarding finding the nearest node.
- Added fade-in to `Travel Map` screen.

### Fixes:
- Resolved background of any GUI screen from appearing black when standing inside a charting node (NeoForge).
- Resolved vignette overlay from darkening when in pause screen (NeoForge).
- Resolved linked signs not unlinking when broken by the player (Forge/NeoForge).

### Changes:
- Set `Biome Fallback` config option to false by default
- Reduced default `Travel Fatigue` effect duration to `6s`

### Notes:
- If none of your other mods use `Common Network` feel free to remove it.