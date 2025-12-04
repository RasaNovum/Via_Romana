# Via Romana 2.2.0 Changelog:

### Features:
- Added the ability to `sneak & right-click` anywhere along a charted path to open the `Map Screen` using the `Charting Map` item.
  - Added `Enable Remote Map Access` config option allowing users to disable this feature.
- Added movement speed boost while walking on charted paths
  - Added `Path Movement Speedup` config option allowing users to customize (or disable) the speedup.
  - This approach uses proximity to path nodes which avoids issues traditional speedup mods have that work by targeting walked-on blocks which would apply to non-path scenarios like inside buildings.

### Improvements:
- Ported networking from `Common Network` to `Data Anchor` to remove a dependency
  - New packet registration system roughly halves the amount of code per-packet making maintenance much easier than before.
  - TODO: Force Data Anchor version
- Added `BOOK_PAGE_TURN` sound effect to map screen opening.
- Performance improvements regarding finding the nearest node.

### Changes:
- Set `Biome Fallback` config option to false by default
- Reduced default `Travel Fatigue` effect duration to `6s`

### Notes:
- If none of your other mods use `Common Network` feel free to remove it.