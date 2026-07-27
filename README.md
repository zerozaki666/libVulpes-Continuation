# LibVulpes Continuation

LibVulpes Continuation is a maintained Minecraft 1.7.10 library for
AdvancedRocketry Continuation and other compatible mods.

## Features

- Multiblock structure projector for vanilla and mod-provided structures.
- Public API for registering projector-only multiblock descriptions.
- XML machine recipes with item metadata, Ore Dictionary entries, fluids, and
  optional item NBT.
- LibVulpes Dilithium crystals registered as `gemDilithium` for
  Ore-Dictionary-based FTL fuel consumers.
- Basic structure blocks, motors, reusable machine bases, energy adapters, and
  modular GUI components.
- Continued bug fixes for recipe loading, networking, rendering, and
  null-safety.

GT6-, TerraFirmaCraft-, modpack-specific, and Dyson-sphere/cloud integrations
from the TFRU fork are intentionally not included.

See [`TFRU_COMMIT_AUDIT.md`](TFRU_COMMIT_AUDIT.md) for the decision and
adaptation applied to every one of the 32 source-only commits.

## Upstream and license

This continuation is based on the original
[LibVulpes](https://github.com/Advanced-Rocketry/libVulpes) and incorporates
adapted work from
[libVulpes-TFRU](https://github.com/kuzuanpa/libVulpes-TFRU).

The combined continuation is distributed under GNU AGPL v3. The original
LibVulpes code and notice remain available under the MIT license; see
`LICENSE-MIT`.
