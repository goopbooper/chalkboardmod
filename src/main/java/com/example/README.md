[ PLAYER PLACES BLOCK ITEM ]
            │
            ▼
┌────────────────────────────────────────────────────────┐
│               1. MyWallArtBlock.java                   │
│  • Scans the wall to find the max available space      │
│  • Spawns helper pieces across the grid positions      │
│  • assigns custom properties: FACING & PART            │
└───────────────────────────┬────────────────────────────┘
                            │
              Creates the world blocks & triggers...
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│             2. WallArtBlockEntity.java                 │
│  • Generates a data instance attached to the position  │
│  • Stores your unlimited data string (e.g., image URL) │
│  • Saves/Loads NBT data to your world file             │
└───────────────────────────┬────────────────────────────┘
                            │
              Syncs data packets over network to...
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│             3. WallArtRenderState.java                 │
│  • Gathers client-side data right before drawing       │
│  • Extracts current properties (FACING, PART, texture) │
└───────────────────────────┬────────────────────────────┘
                            │
              Feeds snapshot parameters into...
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│               4. WallArtRenderer.java                  │
│  • Bypasses the empty "art_panel.json" completely      │
│  • Reads state.part to slice texture coords (U, V)     │
│  • Manually builds and renders the thin 3D polygon     │
└───────────────────────────┬────────────────────────────┘
                            │
               Pushes vertex data to GPU
                            │
                            ▼
               [ VISUAL REFRESH ON SCREEN ]
