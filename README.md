# ResourceToggle

A client-side Minecraft 26.2 mod that lets you instantly toggle all non-vanilla resource packs in-game for testing/debugging.

## Features

- **Toggle button** on the title screen and pause screen (top-right corner).
- **Hotkey** in Controls → Misc (unbound by default).
- **Action bar feedback** on switch.
- **Server never sees the switch**: packs are always accepted, downloaded and
  reported as applied to the server (including required server packs); only the *rendered* resource stack switches. No packets are sent, no repository
  selection is changed.
- **Three switch modes**, configurable in `config/resourcetoggle.json`:

| mode            | behaviour                                                                                                                |
|-----------------|--------------------------------------------------------------------------------------------------------------------------|
| `gpu`           | Both resource stacks are pre-loaded; the switch keeps the alternate stack warm in memory and only re-uploads to the GPU. |
| `ram` (default) | Both stacks are cached in RAM; switching never touches disk/zip/network again after the first warm-up.                   |
| `on_demand`     | A regular vanilla-style reload on every switch (lowest memory use).                                                      |

In `ram`/`gpu` mode the first switch in a session is still a full load; every
following switch is served from memory and should be noticeably faster than a normal
F3+T reload.


> Note on "instant": 26.2's new render pipeline (Vulkan/GL render graph,
> `RenderPipeline`, atlas re-upload on apply) re-uploads textures during the
> reload's apply phase regardless of what the mod does. Keeping two fully live
> render graphs simultaneously is not practical, so the `gpu` mode provides the
> warm-stack + minimal-GPU-work switch rather than a true zero-cost pointer
> swap.

## Config

`config/resourcetoggle.json` (created on first run):

```json
{
  "mode": "ram",
  "keepLoaderPacks": true,
  "toastOnToggle": true,
  "extraKeepPacks": [],
  "extraExcludePacks": []
}
```

- `mode`: `"gpu"`, `"ram"` or `"on_demand"`.
- `keepLoaderPacks`: keep mod-provided assets in vanilla-only mode (required
  for most mods to render).
- `extraKeepPacks` / `extraExcludePacks`: force packs in or out of the
  vanilla-only stack by pack id.
- `toastOnToggle`: show the action-bar message on switch.

## Building

Requires JDK 25.

```bash
./gradlew build
```

Output: `build/libs/ResourceToggle-1.0.jar` — works on both Fabric and
NeoForge clients. Drop it into the mods folder. No Fabric API or other mods required.
