# Via Romana 2.2.2 Changelog:

### Features:
- Added `GPU path rendering` to the `Travel Map Screen`.
    - This resolves the lag while previewing the map of massive path networks with many curves.

### Changes:
- Removed Surveyor (Antique Atlas/Hoofprint) integration for the time being to avoid a crash. I plan on adding a better approach in the future, but since I don't plan on keeping the current system I don't want to maintain it.

### Notes:
- Moonlight Lib versions `1.20-2.18`, `1.20-2.16.19` and `1.20-2.20` break block tag generation for Fabric 1.20.1, either backport to `1.20-2.16` or use `1.20-2.21+` when it's released as it will fix the issue.