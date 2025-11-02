# Via Romana 2.1.1 [Neo/Forge & Fabric] Changelog:

### Fixes:
- Resolved server hang on /stop command usage
- Resolved maps not updating upon chunk changes in singleplayer
- Resolved certain tags not being found due to mod conflicts causing failed tag construction
  - Switched to using strings instead of tags where possible, tags are now intended to be added by the player or modpack developer
  - Config regeneration required for changes to occur
- Corrected some translation key usage