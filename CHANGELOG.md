# Via Romana 2.1.0 [Neo/Forge & Fabric] Changelog:

### Changes:
- Via Romana now has native builds for Forge and NeoForge which avoids any issues people were having with Sinytra Connector - [#58](https://github.com/RasaNovum/Via_Romana/issues/58)
- Via Romana is no longer using Surveyor Map Framework for map info, we have our own ground-up system which both adds functionality and fixes crashes - [#76](https://github.com/RasaNovum/Via_Romana/issues/76) [#70](https://github.com/RasaNovum/Via_Romana/issues/70) [#60](https://github.com/RasaNovum/Via_Romana/issues/60) [#59](https://github.com/RasaNovum/Via_Romana/issues/59)
- We now use Moonlight for dynamic tag generation (instead of BRRP), Data Anchor for saving, and Common Network for networking
  - This means existing path networks aren't directly transferable to this update, but can be migrated using `/viaromana convert legacyPaths` (all networks will be saved to the overworld)

### Additions:
- Added an estimated biome background layer which makes larger maps look much more complete
- Added 

### Fixes:
- Resolved Travellers Fatigue not applying as expected
- Resolved padding issues with maps
- Resolved nodes not saving to a specific dimension but instead globally
- Resolved messages not correctly reflecting charting radius
- Resolved invalid block indicator not correctly reflecting charting radius
- Resolved nodes not being placed on the liquid blocks when added to valid path tag list
- Resolved nodes not scanning up for ideal placement
- Improved map background tile blending