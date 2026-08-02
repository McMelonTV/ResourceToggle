# ResourceToggle

A client-side Minecraft 26.2 mod that ships as a **single jar for both Fabric
and NeoForge**. It lets you instantly switch between your full resource pack
set and **vanilla-only resources** with one keypress or a click on the
title/pause screen — without the server ever noticing.

## Features

- **Toggle button** on the title screen and pause screen (top-right corner).
- **Hotkey** in Controls → Misc (unbound by default — assign one, e.g. `K`).
- **Instant action-bar feedback** on every switch.
- **Server never sees the switch**: packs are always accepted, downloaded and
  reported as applied to the server (including required server packs); only the *rendered* resource stack switches. No packets are sent, no repository
  selection is changed.
- **Three switch modes**, configurable in `config/resourcetoggle.json`:

  | mode | behaviour |
      |------|-----------|
  | `gpu` | Both resource stacks are pre-loaded; the switch keeps the alternate stack warm in memory and only re-uploads to the GPU. |
  | `ram` (default) | Both stacks are cached in RAM; switching never touches disk/zip/network again after the first warm-up. |
  | `on_demand` | A regular vanilla-style reload on every switch (lowest memory use). |

  In `ram`/`gpu` mode the first switch in a session is still a full load; every
  following switch is served from memory and is noticeably faster than a normal
  F3+T reload.

## How it works

26.2 ships its client jar pre-mapped (no obfuscation), and the whole reload
pipeline is driven by `Minecraft#reloadResourcePacks`. The mod:

1. Intercepts the pack list handed to the reload (a single redirect in
   `Minecraft.reloadResourcePacks`).
2. When vanilla-only mode is active, filters that list down to the vanilla
   pack plus mod-provided assets. Everything else is dropped from the render
   stack but stays *selected* in the repository. The filter is loader-agnostic
   and relies on vanilla id conventions: user packs are `file/*`, server packs
   are `server/*`, built-ins have fixed ids, and everything else is mod
   provided.
3. Wraps the surviving packs in a byte-cache so repeated reloads read from
   memory.
4. Because the repository state never changes, `DownloadedPackSource` keeps
   reporting `SUCCESSFULLY_LOADED` to the server exactly as in vanilla, and
   required server packs can never kick you while toggled.

Every other reload source (F3+T, joining a server, a server pushing packs
mid-game) respects the active mode — the switch cannot silently revert.

### Single cross-loader jar

The jar contains **both** `fabric.mod.json` and `META-INF/neoforge.mods.toml`,
plus two tiny entrypoints (`@Mod` class for NeoForge, client entrypoint for
Fabric). All runtime behaviour comes from mixins against pure vanilla classes,
so no loader APIs are used:

- keybinding → mixin into `Options.<init>` (final `keyMappings` array via
  `@Mutable` accessor)
- client tick → mixin into `Minecraft.tick`
- config dir → `Minecraft.gameDirectory` (both loaders use `<gamedir>/config`)

The NeoForge annotations are compiled against stubs in `src/stubs` which are
never packaged. On Fabric the `@Mod` class is simply never loaded; on NeoForge
the fabric entrypoint is never loaded.

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
NeoForge clients. Drop it into the mods folder. It works on vanilla servers,
Paper, etc. No Fabric API or other mods required.

## License

MIT
