# Via Romana 2.1.3 Changelog:

### Improvements:
- Changed config `spline_animation_speed` to `spline_animation_time` which now reflects the time it takes for any network to be fully animated. This results in the ability to animate paths significantly faster than before.
- Added map optimizations for chunk image setting & getting.
- Added spatial lookup for map updates to improve performance upon block changes.
- Improved block update check logic to return earlier in most cases improving performance.
- Reduce PathGraph sync packet size by optimizing Node structure
- Added `/viaromana sync` to force-sync all players PathGraphs in a given dimension.

### Fixes:
- Made PathGraph sync more robust to prevent players from being unable to interact with/view paths on login.
- Resolved block update check skipping block breaking in some cases.