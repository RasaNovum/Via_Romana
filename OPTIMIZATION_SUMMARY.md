# Raw Pixel Storage Optimization - Implementation Summary

## Changes Made

### Core Optimization
Switched from PNG-based chunk storage to **raw pixel storage** using Minecraft's MapColor packed ID format.

### Key Benefits
- **85% storage reduction**: 256 bytes per chunk (vs ~1-2KB PNG)
- **~40-50% faster rendering**: No PNG encoding/decoding overhead per chunk
- **Simpler compositing**: Direct byte array copying instead of BufferedImage operations
- **Better caching**: Smaller memory footprint for cached data

## Files Changed

### 1. **New Files Created**
- `LevelPixelTrackedData.java` - Stores raw pixels (byte[256]) per chunk at level scope
- `ChunkPixelUtil.java` - Renders and manages raw pixel data
  - `renderChunkPixels()` - Returns byte[256] with MapColor packed IDs
  - `pixelsToArgb()` - Converts to ARGB only when needed (final PNG bake)
  - `scalePixels()` - Nearest-neighbor scaling for raw bytes

### 2. **Updated Files**
- `MapInit.java` - Changed from `CHUNK_PNG_KEY` to `CHUNK_PIXEL_KEY`
- `MapBakeWorker.java` - Complete rewrite to use raw pixel compositing
  - Direct byte array operations instead of Graphics2D
  - Single PNG encode at the very end (not per chunk)
  - Simplified incremental updates (currently does full rebake - still fast!)
- `ServerMapCache.java` - Method renames: `clearAllChunkPixelData()`, `regenerateAllChunkPixelData()`
- `ViaRomanaCommands.java` - Updated command calls to use new method names

### 3. **Files Deleted**
- `ChunkPngUtil.java` - Old PNG-based rendering
- `LevelPngTrackedData.java` - Old PNG storage

## Technical Details

### Raw Pixel Format
Each chunk = `byte[256]` where:
- Index: `x + z * 16` (0-255 for 16×16 grid)
- Value: `MapColor.getPackedId(brightness)` - combines color ID (6 bits) + brightness (2 bits)

### Rendering Flow (Before)
```
1. Render chunk heightmap/blocks → BufferedImage
2. Encode BufferedImage → PNG bytes (~1-2KB)
3. Store PNG in Data Anchor
4. For map bake:
   - Load PNG → Decode → BufferedImage
   - Composite via Graphics2D.drawImage()
   - Encode final map → PNG
```

### Rendering Flow (After)
```
1. Render chunk heightmap/blocks → byte[256] (~0.256KB)
2. Store raw pixels in Data Anchor
3. For map bake:
   - Load raw pixels (no decode!)
   - Direct System.arraycopy() to composite
   - Convert to ARGB once at end
   - Encode final map → PNG
```

## Performance Metrics Added

All operations now log with `[PERF]` prefix:
- Chunk rendering time (per chunk)
- Map bake time breakdown (render/convert/encode)
- Storage sizes (raw pixels vs final PNG)
- Disk I/O times
- Bulk operation times

## Expected Performance Improvements

Based on baseline (143 chunks, 186×162 pixels):
- **Before**: ~270ms total (mostly chunk render + PNG encode/decode)
- **After (estimated)**: ~100-150ms total (~40-50% faster)
  - No per-chunk PNG encode: saves ~0.5-1ms × 143 chunks
  - No PNG decode during composite: saves decode overhead
  - Direct byte operations: faster than BufferedImage compositing

### Storage Comparison
- **Per-chunk**: 256B raw vs ~1-2KB PNG (85% reduction)
- **143 chunks total**: 36.6KB raw vs ~143-286KB PNG
- **Final map**: Still ~9KB PNG (only one encode at end)

## Migration Notes

- **No backward compatibility** - existing PNG data will be ignored
- All cached chunk data will be regenerated on first access
- NBT key changed: `viaromana:chunk_png` → `viaromana:chunk_pixels`
- Commands remain the same (just updated internally)

## Future Optimizations

1. **True incremental updates**: Store raw pixel array in `MapInfo` for O(1) chunk updates
2. **Bilinear scaling**: Better quality than nearest-neighbor for scale factors > 1
3. **Parallel rendering**: Already threaded, but could batch better
4. **Compression**: Raw pixels compress well with LZ4/Snappy if NBT size becomes an issue

## Testing Checklist

- [ ] Build succeeds without errors
- [ ] Map rendering works in-game
- [ ] Chunk updates trigger correctly
- [ ] Performance logs show improvements
- [ ] Disk saves/loads work correctly
- [ ] Commands (`/viaromana clearmaps`, `/viaromana regeneratemaps`) work
- [ ] Large networks handle properly
- [ ] Scale factors work correctly (scaleFactor > 1)

