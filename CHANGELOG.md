# Via Romana 2.2.2 Changelog:

### Features:
- Added `GPU path rendering` to the `Travel Map Screen`.
    - This resolves the lag while previewing the map of massive path networks with many curves.

### Changes:
- Removed `Surveyor` mod (Antique Atlas/Hoofprint) map integration for the time being to avoid a crash. I plan on adding a better approach in the future, but since I don't plan on keeping the current system I don't want to maintain it.
- Running `/viaromana maps clear biomePixels` will reset the biome colour cache for easier editing of `Biome Color Pairs` config option without `/reload` or re-logging.

### Notes:
- Moonlight Lib versions `2.16.17`, `2.16.18`, `2.16.19` and `2.16.20` break block tag generation for `Fabric 1.20.1`, either backport to `2.16.16` or use `2.16.21+` when it's released as it will fix the issue.