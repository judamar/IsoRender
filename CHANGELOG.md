# Changelog

All notable changes to IsoRender are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-11-14

First public release. Minecraft 1.12.2 with LiteLoader 1.12.2; MIT license.

### Added

- **Region selection** with `/isorender pos1`, `/isorender pos2` and `/isorender clear`, on the aimed block (or the block under your feet), with a yellow in-world outline of the selection box. Regions up to 64 × 64 × 64 blocks, validated before rendering (points set, size, loaded chunks, not only air).
- **Isometric rendering** (rotation 45°, elevation 35.264° by default) and a **perspective view** (fixed 30° field of view), with automatic framing and a 5% margin for any rotation and elevation.
- **Real alpha output**: transparent background, correct translucency (glass, stained glass, ice, water), biome tint, and supersampled edge smoothing without halos. Square PNG from 512 to 4096 px (default 512).
- **Interactive menu** (key **K** or `/isorender menu`) with sliders and numeric fields for rotation, elevation, zoom, offset X and offset Y, a resolution selector, and **Render**, **Close** and **Reset** buttons.
- **Live preview** in the menu, with a simplified mode for regions larger than 32 × 32 × 32 blocks.
- **Camera presets**: Isometric, Front and Top.
- **Preview background** selector (Checkerboard, White, Gray, Black, Chroma green); it affects only the preview, never the exported PNG.
- **Height cut** (Cut Y) to hide the layers above a given Y in both the preview and the PNG.
- **`/isorender item [id] [meta]`**: renders the item in your hand (or the off hand), or an item/block by ID and metadata, as it looks in the inventory.
- **Block entities** (chests, beds, signs, heads, banners and similar) and **item frames, paintings and armor stands** are drawn.
- **Configuration file** `liteconfig/common/isorender.properties` (`outputFolder`, `resolution`, `rotation`, `elevation`, `zoom`, `offsetX`, `offsetY`, `view`, `background`, `maxSize`); the menu remembers the last camera, view, resolution and background. An invalid value resets only that key to its default and is logged.
- **Unique file names**: `structure_<yyyyMMdd-HHmmss>_<x1,y1,z1>_<x2,y2,z2>.png` and `item_<yyyyMMdd-HHmmss>_<id>_<meta>.png`, with `_2`, `_3`... on collision. Clickable path in the chat after each render.
- Debug commands `/isorender test`, `/isorender test-multi` and `/isorender stress`, which write to `isorender-test/`.
- User-facing text (chat, menu, errors) in English.

### Security and robustness

- **Read-only:** the mod never modifies blocks or entities in the world (held items are copied before rendering).
- **No game crashes from the mod:** a failure in selection, render or menu is reported in the chat with a short message (details in the log) and the graphics state is restored. Memory is estimated before rendering and large requests are rejected or reduced (no edge smoothing) instead of failing with an out-of-memory error. A half-written PNG is never left behind. Only one render runs at a time.

### Known limitations

- Regions are limited to 64 × 64 × 64 blocks and to the chunks loaded by your client.
- Only vanilla blocks are guaranteed; blocks from other mods are best-effort.
- Only item frames, paintings and armor stands are drawn as entities (besides block entities): no mobs, players, dropped items or leads.
- A region containing only an armor stand or an item frame and no block is rejected as "only air".
- Fixed full lighting; the background is always transparent and the image always square.
- `/isorender` is case-sensitive and has no Tab completion. An empty resolution field means 512.
- A Y cut outside the region is ignored without a warning; the cut is not saved and Reset does not clear it.
- The configuration file is rewritten in full when the menu closes; editing it while the game is open has no effect.
- The debug commands write to `isorender-test/` and ignore `outputFolder`.
- The preview is simplified (lower quality, no entities) for regions larger than 32 × 32 × 32 blocks.
- 4096 px with edge smoothing needs a GPU with an 8192 px maximum texture size; otherwise it is drawn without smoothing. Video memory is only checked in advance on NVIDIA GPUs.
- Tested on a vanilla 1.12.2 client with LiteLoader and no other mods; compatibility with other mods or with Forge is not guaranteed.

[1.0.0]: https://github.com/judamar/IsoRender/releases/tag/v1.0.0
