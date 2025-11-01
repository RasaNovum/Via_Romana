# Via Romana 2.1.0 [Neo/Forge & Fabric] Changelog:

### Changes:
- Via Romana now has native builds for Forge and NeoForge which avoids any issues people were having with Sinytra Connector - [#58](https://github.com/RasaNovum/Via_Romana/issues/58)
- Via Romana is no longer using Surveyor Map Framework for map info, we have our own ground-up system which both adds functionality and fixes crashes - [#76](https://github.com/RasaNovum/Via_Romana/issues/76) [#70](https://github.com/RasaNovum/Via_Romana/issues/70) [#60](https://github.com/RasaNovum/Via_Romana/issues/60) [#59](https://github.com/RasaNovum/Via_Romana/issues/59)
  - Instead of Surveyor always scanning all chunks, chunks are now scanned during the charting process resulting in significantly lower storage utilization and performance impacts during normal use
  - Via Romana is still compatible on Fabric with Surveyor-based mods like Hoofprint and Antique Atlas for destination additions on maps
- We now use Moonlight for dynamic tag generation (instead of BRRP), Data Anchor for saving, and Common Network for networking
  - This means existing path networks aren't directly transferable to this update, but can be migrated using `/viaromana convert legacyPaths` (all networks will be saved to the overworld).
  - Deleting your existing `via_romana.json` config file is strongly recommended.

### Additions:
- Added an estimated biome background layer which makes larger maps look much more complete
  - Added a config for defining custom config colours, useful for both overriding defaults or adding modded biomes (please submit a Github issue if a biome is missing that should be included)
- Added a fallback to biome data when a map is large enough for the added resolution of full chunk data to be diminishing, resulting in a significant speed boost for large map generation


### Fixes/Improvements:
- Resolved traveler's Fatigue not applying as expected
- Resolved padding issues with the map screen
- Resolved nodes not saving globally instead of to a specific dimension
- Resolved status messages not correctly reflecting charting radius
- Resolved tutorial not correctly reflecting charting radius
- Resolved invalid block indicator not correctly reflecting charting radius
- Resolved nodes not being placed on the liquid blocks when added to valid path tag list
- Resolved nodes not scanning upwards for ideal placement
- Improved map background tile blending
- Improved node vignette when walking through charting nodes
- Improved underground detection for map screen path spline
- Improved default tag/block inclusions in `via_romana:path_block`
- Saving config via GUI automatically updates caches (1.21.1 exclusive)
- Tweaked default config values