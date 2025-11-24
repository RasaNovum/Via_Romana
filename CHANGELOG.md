# Via Romana 2.1.3 Changelog:

### Improvements:
- Changed config `spline_animation_speed` to `spline_animation_time` which now reflects the time it takes for any network to be fully animated. This results in the ability to animate paths significantly faster than before.
- Added map optimizations for chunk image setting & getting.
- Added spatial lookup for map updates to improve performance upon block changes.
- Improved block update check logic to return earlier in most cases improving performance.
- Reduce PathGraph sync packet size by optimizing Node structure
- Added `/viaromana sync` to force-sync all players PathGraphs in a given dimension.
- Altered `Logging Level` states from `NONE, DEBUG, VERBOSE` to `NONE, ADMIN, DEBUG` to clarify intended usage, if you were previously using `VERBOSE` it will default to `NONE`.
  - Generally improved which logs belong to which logging state.
  - Added logging to admin-relevant events such as sign links/unlinks.
  - Moved `Logging Level` config from `Map` Screen to `Visuals` Screen, which still isn't exactly correct but closer.

### Fixes:
- Made PathGraph sync more robust to prevent players from being unable to interact with/view paths on login.
- Resolved block update check skipping block breaking in some cases.
- Resolves block tags not generating when using `MidnightLib v1.9.0+`

### Notes:
- Update to the newest Data Anchor version to resolve a TPS issue with Enhanced Celestials
- Update to the newest Data Anchor version to resolve a crash related to `dataanchor$onTickBlockEntities`