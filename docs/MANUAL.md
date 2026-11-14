# IsoRender user manual (T-038)

Mod version: 1.0.0 · Minecraft 1.12.2 · LiteLoader 1.12.2 · MIT license

Requirements covered: RF-003 (selection), RF-016 (menu), RF-028 (configuration). For people who work on the mod's code, see `docs/TECNICA.md`. The text shown by the mod (chat, menu, errors) is in English; the internal project documents `ERS.md`, `PLAN.md` and `CASOS-PRUEBA.md` are in Spanish.

## 1. What IsoRender does

IsoRender turns a build in your world into a PNG image with a transparent background. You mark two corners (point 1 and point 2), open a menu with a live preview, adjust the camera and press **Render**. The mod saves the PNG in a folder of your game and prints the path in the chat.

- The default view is isometric (rotation 45°, elevation 35.264°). A perspective view is also available.
- The image is square, from 512 to 4096 pixels per side, with smoothed edges and no halos.
- Blocks, block entities (chests, beds, signs, heads...) and item frames, paintings and armor stands are drawn.
- The mod is read-only: it **never changes blocks or entities in your world**.
- If something fails (selection, render or menu), it tells you in the chat and the game stays open.

## 2. Requirements and installation

| You need | Detail |
|---|---|
| Minecraft | Java Edition 1.12.2 (client) |
| LiteLoader | LiteLoader 1.12.2 installed in that profile. **Forge is not used.** |
| Graphics | A GPU with OpenGL and framebuffer support (any machine that runs 1.12.2). For 4096 px with smoothed edges the GPU needs a maximum texture size of 8192 px (see §11). |

Installation:

1. Install LiteLoader 1.12.2 in your Minecraft 1.12.2 profile (follow LiteLoader's own instructions).
2. Copy `mod-isorender-1.0.0-mc1.12.2.litemod` into the `mods` folder of your game (`<game directory>/mods/`). Do not unzip it.
3. Start the game with the LiteLoader profile. LiteLoader's mod list should show "IsoRender 1.0.0".
4. Enter a world. Each time you enter a world the chat shows `[IsoRender] Mod loaded (v1.0.0).`

The mod is client-side only: the `/isorender` commands are intercepted on your client and are not sent to the server. It was tested in single-player; on a server it can only see the chunks your client has loaded.

## 3. Quick start

1. Aim at one corner of the build and type `/isorender pos1`.
2. Aim at the opposite corner and type `/isorender pos2`. The chat shows the size of the region.
3. Press the **K** key (or type `/isorender menu`) to open the menu.
4. Adjust the camera while watching the preview.
5. Press **Render**. The menu closes and, on the next frame, the mod generates the PNG and writes the path in the chat (the path is clickable: it opens the image).

With a complete selection you can also render without opening the menu: `/isorender render` uses the camera, resolution and Y cut you had in the menu.

## 4. Selecting the region

### 4.1 Setting the points

| Command | What it does |
|---|---|
| `/isorender pos1` | Sets point 1 on the block you are aiming at. |
| `/isorender pos2` | Sets point 2 the same way. |
| `/isorender clear` | Clears both points. Answers "Selection cleared." or "There was no selection to clear." |

- **Aimed block:** if your crosshair is on a block (within normal interaction reach), that block is used. The chat says `[targeted block]`.
- **Not aiming at a block:** the block you are standing on (the one right under your feet) is used, and the chat says so (`[you are not looking at a block; using the block you are standing on]`).
- Repeating `pos1` or `pos2` replaces the previous point.
- The order does not matter: the region is the box that contains both points, **inclusive**. If both points are the same block, the region is 1 × 1 × 1.
- Example: pos1 at (0, 64, 0) and pos2 at (9, 69, 4) give a region of 10 × 6 × 5 blocks. The chat writes it as `Region: 10 × 6 × 5 blocks.`
- The full confirmation looks like `[IsoRender] Point 1 set at (0, 64, 0) [targeted block].`

### 4.2 Outline

While at least one point is set, the mod draws a yellow outline of the selection box in the world. It is visible from inside and outside, and also through blocks (fainter). It disappears with `/isorender clear`.

### 4.3 Limits and validations

Before rendering (with the button, with `/isorender render` and also in the preview) the mod checks:

| Rule | Detail |
|---|---|
| Two points exist | If one is missing, it tells you which. |
| Maximum size | **64 × 64 × 64** blocks per axis (can be lowered with `maxSize`, never raised above 64; see §9). |
| Loaded chunks | Every chunk the region touches must be loaded in your client. Move closer if they are not. |
| Not only air | There must be at least one block that is not air. |

It is a client mod: it only sees what your client has loaded. On a server, the render distance limits which areas you can capture.

## 5. The menu

It opens with **K** or `/isorender menu` (only if no other screen is open: it does not open over the inventory or the chat). It does not pause the game. Close it with **Esc** or the **Close** button; the K key does not close it. The key binding is listed as "Open IsoRender menu" in the "IsoRender" category of Options > Controls.

Layout from top to bottom:

```
              IsoRender
   Region: 10 × 6 × 5 blocks           (green if the selection is complete; orange if something is missing)
 [ Rotation: 45° .......... ] [ 45 ]     +----------------------+
 [ Elevation: 35.3° ....... ] [35.3]     |                      |
 [ Zoom: 100% ............. ] [ 100 ]    |       PREVIEW        |
 [ Offset X: 0% ........... ] [  0  ]    |                      |
 [ Offset Y: 0% ........... ] [  0  ]    |                      |
 [ Background: Checkerboard ][ View: Isometric ]                 |
 [ Resolution: 512 px ..... ] [ 512 ]    +----------------------+
 [Isometric][Front][Top]                       [   Render   ]
 Cut Y (empty = no cut)   [     ]              [   Close    ]
 Valid range: 64..69
 [Reset]  (error message in red)
```

### 5.1 Camera: rotation, elevation, zoom and offset

Each setting has a slider (drag with the mouse; it moves in whole steps) and a numeric field on the right (accepts decimals).

| Setting | Range | Default | What it does |
|---|---|---|---|
| Rotation | 0 to 359° | 45° | Turns the camera around the structure. |
| Elevation | 5 to 90° | 35.264° | Camera height: 5° is almost level, 90° is straight down. |
| Zoom | 25 to 400% | 100% | Zooms in or out. At 100% the whole structure fits with a 5% margin. |
| Offset X | -100 to 100% | 0% | Moves the structure right (+) or left (-) in the image. |
| Offset Y | -100 to 100% | 0% | Moves the structure up (+) or down (-). |

The offset is measured as a percentage of the image half-width. The structure is framed automatically (centered and with a margin) for any rotation and elevation; zoom and offset are applied on top.

**Numeric input:** click a field, type the value (decimal point or comma) and confirm with **Enter**, with **Esc** (only closes the field, not the menu) or by clicking elsewhere. A value out of range is clamped to the nearest limit. If what you typed is not a number, the field goes back to the previous value and a red message appears next to the Reset button (for example `Rotation is not a valid number: "abc".`).

### 5.2 Preview background

The **Background** button cycles through Checkerboard, White, Gray, Black and Chroma green. The checkerboard shows which areas are transparent. **It only changes the preview:** the exported PNG always has a transparent background. The chosen background is saved in the configuration. If the text does not fit in the button it is shortened (for example `Bg: Checkerboard`).

### 5.3 Isometric or Perspective view

The **View** button toggles between "Isometric" (orthographic projection, no distance distortion; the default) and "Perspective" (fixed 30° field of view; nearby parts look bigger). It affects both the preview and the PNG. In perspective, zoom and offset move the image window; the camera does not move closer. If the text does not fit in the button it is shortened ("View: Persp." / "View: Iso.").

### 5.4 Resolution

The **Resolution** button cycles through 512, 1024, 2048 and 4096 px (from an in-between value it jumps to the next larger one; from 4096 it goes back to 512). The field next to it accepts any integer from **512 to 4096**. Out of range it shows `Resolution out of range: N px. Must be between 512 and 4096 px.` and keeps the previous value; text that is not an integer shows `Invalid resolution: "abc". Use an integer between 512 and 4096 px.` An empty field is equivalent to 512. The image is always square (N × N).

### 5.5 Presets

Three buttons set rotation and elevation at once; they do not change zoom, offset or view:

| Preset | Rotation | Elevation |
|---|---|---|
| Isometric | 45° | 35.264° |
| Front | 0° | 5° (the minimum allowed) |
| Top | 0° | 90° |

### 5.6 Cut Y

Type an **absolute Y** height (the same one you see in F3) and confirm. Only blocks with Y less than or equal to that value are drawn: useful to see the interior by removing the top layers. The preview and the PNG show the cut, and the framing adjusts to what remains visible.

- Empty field = no cut. Under the label, "Valid range: y1..y2" shows the heights of your region.
- A value below y1, or equal to or above y2, is ignored and **everything is drawn** (no error, no message).
- A value that is not an integer shows `Invalid cut Y: "abc". Enter an integer or leave it empty.` in red and keeps the previous one.
- The PNG name uses the cut box (y2 = the cut).
- The cut is **not saved** in the configuration and **Reset does not clear it**. It stays while the game is open, also if you close and reopen the menu.

### 5.7 Reset

Returns the camera to its defaults (rotation 45°, elevation 35.264°, zoom 100%, offset 0 and Isometric view). It does **not** change the background, the resolution or the Y cut.

### 5.8 Render and Close

- **Render** (greyed out if the selection is not complete): closes the menu and generates the PNG with the current camera, resolution and cut. Because the menu closes, you cannot press it twice. If another render is already in progress, the chat says `A render is already running or pending; wait for it to finish.`
- **Close:** closes the menu. Esc does the same when no text field is active.

When the menu closes (by any route) your latest camera, view, resolution and background are saved to `isorender.properties` (only if something changed), and the menu restores them next time, including after restarting the game.

### 5.9 The preview

The box on the right shows the structure live and updates whenever a control moves. Instead of the image it can show a text:

| Text | Meaning |
|---|---|
| `Selection not set (use /isorender pos1 and pos2)` or `Point N is missing (use /isorender posN)` | The selection is incomplete. |
| A validation message | Region too large, chunks not loaded or only air (see §10). |
| `Preparing preview…` | It is reading the region (takes a moment). |
| `A render is in progress; the preview is waiting.` | A real render is running. |
| An error message | The preview failed (for example, out of memory); the menu stays open. |
| `Window too small for the preview` | The window leaves no room for the box (the minimum side is 64 GUI pixels). |

**Simplified mode:** if the region has more than 32 × 32 × 32 blocks of volume (more than 32,768 blocks, after applying the Y cut), the preview lowers its quality to stay smooth: internal image of up to 256 px (instead of 512), it re-sorts transparencies at most every 0.3 s and it **does not draw entities** (chests, beds, frames...). The text `Simplified view (large region)` under the box indicates it. The final PNG is always drawn at full quality and with entities.

The preview is an approximation: the final PNG uses supersampling for edge smoothing.

## 6. Commands

All start with `/isorender` (lowercase) and are handled by your client; they do not reach the server. There is no Tab completion. An unknown subcommand, or extra arguments, shows the usage line:

`[IsoRender] Usage: /isorender pos1 | pos2 | clear | menu | render | test [size 512-4096] [rotation 0-359] [elevation 5-90] [zoom 25-400] [view iso|persp] | test-multi [n 2-8] | stress [n 1-50] | item [id] [meta]`

| Command | What it does |
|---|---|
| `/isorender pos1` / `pos2` | Sets point 1 / 2 (§4). |
| `/isorender clear` | Clears the selection. |
| `/isorender menu` | Opens the menu (same as the K key). |
| `/isorender render` | Renders the region with the camera, resolution and Y cut of the menu. Takes no arguments: to change the resolution use the menu or the configuration. |
| `/isorender item [id] [meta]` | Renders a single item or block (§6.1). |
| `/isorender test [size] [rotation] [elevation] [zoom] [view]` | Debug command (§6.2). |
| `/isorender test-multi [n]` | Debug command (§6.2). |
| `/isorender stress [n]` | Debug command (§6.2). |

### 6.1 `/isorender item [id] [meta]`

Needs no selection and no menu. It draws the item as it looks in the inventory, with a transparent background and the menu's **resolution**; it ignores the menu's rotation, elevation, zoom and view. There is no Y cut.

| Example | Result |
|---|---|
| `/isorender item` | The item in your main hand; if empty, the one in your off hand. If both are empty: `You are not holding any item.` |
| `/isorender item minecraft:diamond_sword` | That item. The `minecraft:` prefix is optional (`stone` also works). |
| `/isorender item minecraft:wool 14` | Red wool: the second argument is the metadata (integer from 0 to 32767). |
| `/isorender item minecraft:stained_glass 3` | Stained glass, translucent. |

Errors: unknown id (`Unknown item or block "x".`), a block with no item form such as water (`The block "water" has no item form and cannot be rendered.`), invalid or out-of-range metadata (`Invalid metadata: "x". Use an integer between 0 and 32767.` / `Metadata out of range: 40000. Must be between 0 and 32767.`), or more than two arguments (`Too many arguments. Usage: /isorender item [id] [meta]`). In every case no file is created. The item in your hand is not modified (a copy is used).

### 6.2 Debug commands

These are development tools; they use the current selection and do not go through the menu. **They write to `<game directory>/isorender-test/`, not to the configured output folder.**

| Command | Arguments | Default | What it does |
|---|---|---|---|
| `/isorender test` | `[size 512-4096] [rotation 0-359] [elevation 5-90] [zoom 25-400] [view iso\|persp]`, in that order | 512, 45, 35.264, 100, iso | One PNG `test-<size>-<milliseconds>.png`. No offset and no cut. |
| `/isorender test-multi` | `[n 2-8]` | 4 | `n` PNGs of 512 px with the rotation spread over 360° (`multi-<stamp>-<i>of<n>-rot<degrees>.png`). The chat reports the geometry build time and the average draw time. |
| `/isorender stress` | `[n 1-50]` | 30 | `n` renders of 512 px, one per frame, without saving files. At the end it reports the average time, memory change and OpenGL errors. |

Examples:

```
/isorender test                         (512 px, default camera)
/isorender test 2048 90 35 100 persp    (2048 px, rotation 90, elevation 35, zoom 100, perspective)
/isorender test 1024 45 35 150 iso
/isorender test-multi 6
/isorender stress 10
```

The view is the fifth argument: `/isorender test 1024 persp` is not valid ("persp" would be read as the rotation). Wrong arguments give a message in the chat (for example `Rotation is not a valid number: "persp".` or `Invalid view: "x". Use iso or persp.`) and no file is created.

## 7. Where PNGs are saved and what they are called

Default folder: **`<game directory>/isorender/`** (with a launcher such as Prism, the instance's `minecraft` folder). It is created automatically if it does not exist. Change it with `outputFolder` (see §9).

| Type | Name | Example |
|---|---|---|
| Structure | `structure_<yyyyMMdd-HHmmss>_<x1,y1,z1>_<x2,y2,z2>.png` | `structure_20261114-152831_-4,4,-4_-2,4,1.png` |
| Item | `item_<yyyyMMdd-HHmmss>_<id>_<meta>.png` (any character of the id other than letters, digits, `_` and `-`, such as `:`, becomes `-`) | `item_20261114-120000_minecraft-diamond_sword_0.png` |

- The coordinates are the minimum and the maximum corner of the region (with the Y cut applied, if any); negative numbers carry a "-".
- Files are never overwritten: if the name already exists (two renders in the same second) `_2`, `_3`... is appended.
- The PNG is 32-bit RGBA, square, with alpha 0 outside the silhouette and partial alpha on glass, water and smoothed edges.
- If the file cannot be written, no half-written PNG is left behind.

When it finishes, the chat shows, for example: `[IsoRender] Render saved (1024x1024 px, 1530 blocks): /path/isorender/structure_...png [2340 ms]`. The path appears once, underlined and clickable (opens the image with your default program). For items: `Item render saved (512x512 px): <path> [N ms]`.

## 8. What is drawn and what is not

| Drawn | Not drawn |
|---|---|
| All blocks in the region, with their real state (orientation, doors, connected fences, stairs, slabs, halves). | Mobs and animals. |
| Translucent blocks (glass, stained glass, ice, water) blended correctly, and biome tint (grass, leaves, water). | Players (including you). |
| Block entities: chests, beds, signs, heads, banners and similar. | Items on the ground, leads and other entities. |
| Item frames (with their item and rotation), paintings and armor stands (with armor and held items). | Blocks outside the region: they do not appear even if they touch the edge. |
| | The special rendering of beacons, structure blocks, mob spawners and moving pistons (block entities with special handling). |

Things worth knowing:

- **Fixed lighting:** the render uses full brightness. The PNG looks the same by day, at night or in a cave. Soft ambient occlusion (shading in the corners) is always on, and leaves are always drawn with the Fancy graphics quality even if your game is set to Fast graphics.
- Faces on the edge of the region are drawn; faces between adjacent blocks are not.
- Fence and pane connections toward the outside of the region are cut (the outside neighbor counts as air).
- An armor stand counts if the block at its position (its feet) is inside the region; an item frame or a painting counts if its anchor block is inside.
- If an entity fails to draw, it is skipped and the chat warns (`Warning: N block entities could not be drawn and were skipped`). After a render with entities, the chat also tells how many were drawn (for example `3 decorative entities drawn` or `2 block entities drawn`).
- Blocks from other mods are best-effort: they may be missing or look wrong (§11).

## 9. Configuration: `isorender.properties`

It is created automatically the first time you start the game, in:

**`<game directory>/liteconfig/common/isorender.properties`**

(The game log, `logs/latest.log`, has a line `Configuration at <path>` with the exact location.) It is a text file (`.properties` format, UTF-8). Edit it with the game **closed**: the mod reads it at startup and rewrites it completely when you close the menu if something changed, so changes made while the game is open are lost. Key names are case-sensitive.

| Key | Values | Default | Example | What it does |
|---|---|---|---|---|
| `outputFolder` | folder path | empty | `outputFolder=renders` | Empty = `<game>/isorender`. A relative path is taken from the game directory; an absolute one is used as is. |
| `resolution` | integer 512 to 4096 | 512 | `resolution=2048` | Side of the image in pixels. |
| `rotation` | 0 to 359 | 45.0 | `rotation=90.0` | Camera rotation in degrees. |
| `elevation` | 5 to 90 | 35.264 | `elevation=30.0` | Elevation in degrees. |
| `zoom` | 25 to 400 | 100.0 | `zoom=150.0` | Zoom in %. |
| `offsetX` | -100 to 100 | 0.0 | `offsetX=10.0` | Horizontal offset in %. |
| `offsetY` | -100 to 100 | 0.0 | `offsetY=-5.0` | Vertical offset in %. |
| `view` | `iso` or `persp` | `iso` | `view=persp` | Isometric or perspective. |
| `background` | `CHECKERBOARD`, `WHITE`, `GRAY`, `BLACK`, `GREEN` | `CHECKERBOARD` | `background=GREEN` | Preview background (does not affect the PNG). |
| `maxSize` | integer 1 to 64 | 64 | `maxSize=32` | Maximum region size per axis. It cannot be raised above 64. |

Example file (the order of the keys in a file written by the mod may differ):

```
outputFolder=
resolution=1024
rotation=45.0
elevation=35.264
zoom=100.0
offsetX=0.0
offsetY=0.0
view=iso
background=CHECKERBOARD
maxSize=64
```

Rules:

- Decimals accept a point or a comma when reading; the mod always writes with a point.
- A missing key, or one with a blank value, uses the default, with no warning.
- **Invalid value** (text where a number goes, out of range, a `view` that is not `iso`/`persp`, an unknown `background`): only that key goes back to its default and the rest is kept. The reason is written to the game log (`logs/latest.log`, lines such as `Configuration: "zoom=500" is not valid (...); using the default value.`), not to the chat. Note that `resolution=1024.0` is not valid (it must be an integer).
- **Corrupt file** (invalid UTF-8, malformed `\u` sequence): all default values are used and a line is written to the log.
- Missing file: it is created with the default values. If it cannot be created or saved (for example, no permission), the game continues and the reason goes to the log.
- Unknown keys are ignored and disappear the next time the mod saves.
- `outputFolder` and `maxSize` are only changed by hand; the menu saves the rest (camera, view, resolution, background).
- The format is `java.util.Properties`: on Windows write paths with forward slashes (`C:/Renders`) or doubled backslashes (`C\:\\Renders`).

## 10. Common messages and what to do

All carry the `[IsoRender]` prefix.

| Message | Cause | What to do |
|---|---|---|
| `No selection: set the points with /isorender pos1 and /isorender pos2.` | You have not set any point. | Set both points. |
| `Point 1 is missing: set it with /isorender pos1.` (or point 2) | Incomplete selection. In the menu, Render is greyed out. | Set the missing point. |
| `The 70 × 10 × 10 region exceeds the maximum allowed size of 64 × 64 × 64 blocks.` | Some axis exceeds the maximum. | Reduce the region or render it in parts. |
| `Part of the region is not loaded; move closer so the chunks load.` | Part of the region is outside the render distance. | Move closer (or raise the render distance) until it loads, then repeat. |
| `The region contains only air: there are no blocks to render.` | There is no block at all. An armor stand or item frame with **no block** in the region gives this message too. | Include at least one block in the region. |
| `The region contains only air.` | The region has blocks, but none of them produced anything to draw. | Include blocks with a visible model. |
| `A render is already running or pending; wait for it to finish.` | You asked for another render before the previous one ended. | Wait a few seconds. |
| `Not enough memory: about X MB are needed and only Y MB are free. Use a smaller region or a lower resolution.` | Little free Java memory before starting. | Lower the resolution or the region; if possible give Java more RAM (`-Xmx` parameter in your launcher). |
| `Not enough memory while exporting the render; use a smaller region or a lower resolution.` | Java ran out of memory during the render. | Same as above. The game does not close. |
| `Warning: low memory for supersampling at N px; drawing without edge smoothing.` | Warning, not an error: the PNG is generated without smoothing. | More RAM or a lower resolution if you want smooth edges. |
| `Warning: your GPU does not support supersampling at N px; edges will not be smoothed.` | Your GPU does not support double the internal resolution (e.g. 4096 px needs an 8192 texture). | Use 2048 px or less for smooth edges. |
| `Warning: not enough memory for supersampling (...). Retrying without edge smoothing.` | The internal image could not be created; the mod retries without supersampling. | Same as above. |
| `Your graphics card does not support N px (maximum M px). Choose a lower resolution.` | The GPU does not support that resolution. | Lower the resolution. |
| `OpenGL error (0x…) while creating the N px framebuffer; possibly out of video memory.` (also `Incomplete framebuffer (0x…) at N px; possibly out of video memory.`) | Not enough VRAM. | Lower the resolution. |
| `Could not save the file (...). Check permissions and disk space.` | Folder without permission, full disk or a path that cannot be created. | Change `outputFolder` to a folder you can write to and check the disk space. No partial file is left. |
| `Unexpected error while exporting the render (ErrorType). Details are in the game console.` | An unforeseen failure. | The game keeps working. Look at `logs/latest.log` and report it to the maintainer. |
| `Usage: /isorender pos1 \| pos2 \| clear \| menu \| render \| test ...` (the full line is in §6) | Mistyped command or extra arguments. | Check §6. |
| `Warning: OpenGL reported an error (GL_...) during the render; it has been cleared.` | OpenGL logged an error that the mod cleared. | If it repeats or you see visual glitches, look at the log. |
| `N blocks could not be drawn and were skipped` | Some blocks (usually from other mods) failed to draw. | Nothing; the rest of the image is fine. |
| `Error drawing the selection outline; disabled until the game restarts: ...` | The outline failed to draw. | Restart the game; renders are not affected. |

## 11. Known limitations

- **Client only, and only what is loaded:** only the chunks your client has loaded are captured.
- **Regions up to 64 × 64 × 64.** Larger is outside version 1.
- **Only vanilla blocks are guaranteed.** Blocks from other mods are best-effort; if a block fails it is skipped and reported (`N blocks could not be drawn and were skipped`).
- **Entities:** only item frames, paintings and armor stands, plus block entities. No mobs, players, items on the ground or leads.
- **A region containing only an armor stand or an item frame and no block is rejected** as "only air", because the validation looks at blocks, not entities.
- **The image is always square and the background always transparent;** there is no opaque background, no cropping to the silhouette, no animations or GIFs.
- **No Tab completion** for `/isorender`, and the command is case-sensitive (the server treats `/ISORENDER` as an unknown command).
- **Fixed light:** it does not reflect the time of day or the world's light. Fence and pane connections to blocks outside the region are cut.
- **Supersampling:** edge smoothing uses an internal render at twice the side; at 4096 px it needs an 8192 px texture and about 0.5 GB of VRAM. If it does not fit, you are warned and it is drawn without smoothing.
- **Video memory:** it is only checked before the render on NVIDIA GPUs (NVX_gpu_memory_info extension). On other GPUs VRAM is not limited in advance and a failure is detected when the framebuffer is created.
- **Simplified preview** for regions of more than 32,768 blocks of volume (no entities, lower quality).
- **The Y cut is not saved** and only supports a maximum Y (there is no minimum Y); a cut outside the region is silently ignored.
- **An empty resolution field means 512.**
- **The configuration file is rewritten in full** when the menu closes, and editing it while the game is open has no effect.
- **The debug commands write to `isorender-test/`** and ignore `outputFolder`.
- **One operation at a time:** renders, tests and stress runs share a lock; a second one is rejected with a warning.
- **The game pauses for a moment while rendering:** the render runs on the drawing thread (a 32³ region at 2048 px should take a few seconds).
- **Compatibility:** tested on a vanilla 1.12.2 client with a clean LiteLoader. It is not guaranteed with other mods or with Forge.

## 12. Troubleshooting

| Symptom | What to try |
|---|---|
| The mod does not appear / no greeting | Check that you use the LiteLoader 1.12.2 profile (not plain vanilla), that the `.litemod` is in `mods/` and that the Minecraft version is 1.12.2. Look in `logs/latest.log` for "IsoRender". |
| `/isorender ...` is sent as a message or gives "unknown command" | Type it in lowercase and with the leading slash. If there is no greeting, the mod is not loaded. |
| The K key does not open the menu | It may be taken by another function: look for "Open IsoRender menu" (category IsoRender) in Options > Controls and rebind it. It does not open the menu if another screen is open (inventory, chat). Alternative: `/isorender menu`. |
| Render is greyed out | The selection is not complete; the status is shown at the top of the menu. |
| The preview says "not loaded" or "only air" | See §10. |
| The image has a black or white background | Your image viewer does not show transparency; open the PNG in an editor or over another background. The PNG has alpha 0 outside the silhouette. |
| Blocks or entities are missing | Look at the chat: it warns about what was skipped. Mod blocks are best-effort. The simplified preview does not draw entities, but the PNG does. |
| I cannot find the PNG | The path is in the chat (clickable). By default: `<game directory>/isorender/`. The debug commands write to `isorender-test/`. |
| The menu does not remember my settings | They are saved when the menu closes. If `isorender.properties` cannot be written, the game continues and the reason is in `logs/latest.log`. |
| The configuration is ignored | Look in `logs/latest.log` (lines starting with "Configuration:"). Each invalid value is replaced by its default; edit with the game closed. |
| Black or odd screen after a render | It should not happen: the mod restores the graphics state. If it does, note the steps and `logs/latest.log` and report it. |
| The game is slow when rendering 4096 px | A short pause is normal; use 2048 or less if your machine is modest. |
