# Via Romana 2.1.3 Changelog:

### Improvements:
- Changed config `spline_animation_speed` to `spline_animation_time` which now reflects the time it takes for any network to be fully animated. This results in the ability to animate paths significantly faster than before.
- Reduce PathGraph sync packet size by optimizing Node structure
- Added `/viaromana sync` to force-sync all players PathGraphs in a given dimension.

### Fixes:
- Made PathGraph sync more robust to prevent players from being unable to interact with paths.