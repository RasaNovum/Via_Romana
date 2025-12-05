# Via Romana 2.2.0 Changelog:

### Features:
- Added `Linked Sign Particles` which indicate which signs are linked to the path network (no more clicking every sign you see in a server).
  - Added `Enable Linked Sign Particles` config option allowing this to be disabled (client-side).
- Added `Custom Cursor` which appears when hovering over a linked sign to better indicate left-clicking as the action required to access the Travel Map.
  - Added `Enable Custom Cursor` config option allowing this to be disabled (client-side).
- Added the ability to `sneak & right-click` anywhere along a charted path to open the `Map Screen` using the `Charting Map` item.
  - Added `Enable Remote Map Access` config option allowing users to disable this feature.
- Added movement speed boost while walking on charted paths
  - Added `Path Movement Speedup` config option allowing users to customize (or disable) the speedup.
  - This approach uses proximity to path nodes which avoids issues traditional speedup mods have that work by targeting walked-on blocks which would apply to non-path scenarios like inside buildings.

### Improvements:
- Ported networking from `Common Network` to `Data Anchor` to remove a dependency
  - New packet registration system roughly halves the amount of code per-packet making maintenance much easier than before.
- Added `BOOK_PAGE_TURN` sound effect to map screen opening.
- Performance improvements regarding finding the nearest node.

### Changes:
- Set `Biome Fallback` config option to false by default
- Reduced default `Travel Fatigue` effect duration to `6s`

### Notes:
- If none of your other mods use `Common Network` feel free to remove it.