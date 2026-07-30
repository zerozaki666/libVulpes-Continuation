# LibVulpes Continuation 0.2.11 Release Notes

LibVulpes Continuation 0.2.11 adds the generic multiblock lifecycle support
needed by the Kerr black hole generator backport in AdvancedRocketry
Continuation. The black hole generator and its gameplay logic remain in
AdvancedRocketry; this release only provides the reusable LibVulpes support
layer.

## Changes

- Added `TileMultiBlock#getItemInPorts()` and `getItemOutPorts()`.
  - Refreshes loaded TileEntity-backed item-port references from the world.
  - Preserves cached references when a port chunk is unloaded or no replacement
    inventory is available.
  - Avoids loading chunks while refreshing the cache.
- Hardened power-port lifecycle handling.
  - `TilePlugBase#onChunkUnload()` now invalidates its multiblock controller on
    the server.
  - Prevents a completed multiblock from continuing to use stale transient
    power-port references after a port chunk unloads.
- Added the Kerr black hole backport support specification under `doc/`,
  documenting the API, lifecycle, energy, NBT, GUI, and networking contracts
  shared with AdvancedRocketry Continuation.

## Compatibility

- Minecraft 1.7.10
- Forge 10.13.4.1614
- Java 8
- Intended for use with AdvancedRocketry Continuation 1.4.2

## Upgrade Notes

This is a backward-compatible library update and does not require configuration
or world migration. Modpack authors should update LibVulpes Continuation and
AdvancedRocketry Continuation together when using the backported black hole
generator.
