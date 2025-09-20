First Opening of screen:
```
...
[21:43:53] [Server thread/INFO] (Minecraft) [STDOUT]: [DEBUG] TeleportHelper.getDestinations: Found 5 destinations
[21:43:53] [Render thread/INFO] (Minecraft) [STDOUT]: TeleportMapScreen.init() called - data model with 5 destinations
[21:43:53] [Render thread/INFO] (Minecraft) [STDOUT]: SimpleMapRenderer: Created for bounds BlockPos{x=-462, y=0, z=467} to BlockPos{x=-253, y=0, z=701}
[21:43:53] [Render thread/INFO] (Minecraft) [STDOUT]: MapRenderer instance after init: net.rasanovum.viaromana.client.gui.MapRenderer@780e1342
[21:43:53] [Render thread/INFO] (Minecraft) [STDOUT]: SimpleMapClient: Requesting texture for bounds BlockPos{x=-462, y=0, z=467} to BlockPos{x=-253, y=0, z=701}
[21:43:53] [Server thread/INFO] (Minecraft) [STDOUT]: SimpleMapHandler: Generating map for bounds BlockPos{x=-462, y=0, z=467} to BlockPos{x=-253, y=0, z=701}
MapBakeWorker: Generating 224x240 map using PURE Surveyor
[21:43:53] [Server thread/INFO] (Minecraft) [STDOUT]: MapBakeWorker: Processed 210 chunks, 210 had Surveyor data
[21:43:53] [Server thread/INFO] (Minecraft) [STDOUT]: MapBakeWorker: Successfully generated map using Surveyor
[21:43:53] [Server thread/INFO] (Minecraft) [STDOUT]: SimpleMapHandler: Sent map response (25572 bytes)
[21:43:53] [Render thread/INFO] (Minecraft) [STDOUT]: SimpleMapClient: Loaded texture for -462,467:-253,701 (224x240)
```

Second Opening of screen:
```
...
[21:43:55] [Server thread/INFO] (Minecraft) [STDOUT]: [DEBUG] TeleportHelper.getDestinations: Found 5 destinations
[21:43:55] [Render thread/INFO] (Minecraft) [STDOUT]: TeleportMapScreen.init() called - data model with 5 destinations
[21:43:55] [Render thread/INFO] (Minecraft) [STDOUT]: SimpleMapRenderer: Created for bounds BlockPos{x=-462, y=0, z=467} to BlockPos{x=-255, y=0, z=701}
[21:43:55] [Render thread/INFO] (Minecraft) [STDOUT]: MapRenderer instance after init: net.rasanovum.viaromana.client.gui.MapRenderer@3dbff242
```