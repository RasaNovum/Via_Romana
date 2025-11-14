# Via Romana 2.1.2 [Neo/Forge & Fabric] Changelog:

### Improvements:
- Added Russian translations (thanks `@thecooldie`!).
- Added `map_refresh_threshold` config value which allows the user to set a minimum amount of chunks before a map update is allowed. This prevents frequent low-chunk count updates as it's less efficient to update a small amount of chunks due to per-refresh overhead (i.e. 1 chunk and 10 chunks both take ~1.5ms to update in large networks).
- Added `logging_enum` config value which allows the user to specify how much logging will be done during mapping processes.
- Invisible blocks now scan below themselves until a visible block is located.
- Vastly reduced amount of chunk invalidations caused by block changes that would not be visibly changed in the map view.
  - This primarily covers skipping blocks that remain the same map colour and blocks that have no collision (e.g. snow layers or grass).
- Added optimization for when config option `use_biome_fallback_for_lowres` is true, path updates will be disabled for large enough path networks (though chunk data is still invalidated to prevent out-of-date chunk information from persisting). This avoids re-rendering network maps for not visible data.
- Added disabling of `map_refresh_interval`, `map_refresh_threshold` and `map_save_interval` by setting each to `0` in config.

### Fixes:
- Resolved invisible blocks (like glass) from 'bleeding' onto map screen due to layer feather.
- Resolved blocks with no assigned map color from being displayed as invisible.
- Resolved a hang on world/server close when debug logging was enabled.
- Resolved an issue where PathGraph was not loaded to the client on world join by delaying until Data Anchor is initialized.
- Resolved mixin not adjusting Supplementaries Sign Post 'Done' button to the correct position when using non-Fabric loaders

### Investigating:
- I am aware of the Data Anchor `onTickBlockEntities` crash, this update is reported to have potentially fixed it. Please let me know if it does or doesn't as it's been hell to recreate it for testing. I've taken exact worlds and modpacks from players who frequently crashed for it to be fine on my end.