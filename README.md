# Lunge Swap Optimizer (Fabric, Minecraft 1.21.11)

Press G (rebindable): throws a wind charge, swaps to your Lunge spear 3 ticks later,
then jabs 3 ticks after that. Needs a wind charge and a Lunge spear in your hotbar.
Delay is `delay_ticks` in `config/lungeswap.properties` (default 3). Client-side only.

All files sit in one folder (no src/ needed). Build with GitHub Actions
(.github/workflows/main.yml) and use the jar from the artifact.

Install: put the jar and Fabric API in `mods/`.
