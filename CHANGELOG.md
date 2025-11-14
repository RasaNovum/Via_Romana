# Via Romana 2.1.2 [Neo/Forge & Fabric] Changelog:

### Fixes:
- Resolved invisible blocks from 'bleeding' onto map screen due to layer feather.
- Resolved blocks with no assigned map color from being invisible.
- Resolved a hang on world/server close when debug logging was enabled.
- Resolved an issue where PathGraph was not loaded to the client on world join by delaying until Data Anchor is initialized.

### Improvements:
- Added Russian translations (thanks @thecooldie!).
- Invisible blocks now scan below themselves until a visible block is located.
- Added `map_refresh_threshold` to config which allows the user to set a minimum amount of chunks before a map update is allowed. This prevents frequent low-chunk count updates as it's less efficient to update a small amount of chunks due to per-refresh overhead.
- Added optimization for when config option `use_biome_fallback_for_lowres` is true, path updates will be disabled for large enough path networks (though chunk data is still invalidated to prevent out-of-date chunk information from persisting). This avoids re-rendering network maps for not visible data.

### Investigating:
- I am aware of the Data Anchor `onTickBlockEntities` crash, this update is reported to have potentially fixed it. Please let me know if it does or doesn't as it's been hell to recreate it for testing. I've taken exact worlds and modpacks from players who frequently crashed for it to be fine on my end.