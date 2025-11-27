# Via Romana 2.1.4 Changelog:

### Changes:
- Set Biome Fallback to false by default
- Ported networking from `Common Network` to `Data Anchor` to remove a dependency
  - New packet registration system roughly halves the amount of code per-packet making maintenance much easier than before.

### Notes:
- If none of your other mods use `Common Network` feel free to remove it.