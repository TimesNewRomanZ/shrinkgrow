# Shrink & Grow (Minecraft 1.7.10 / GTNH)

A private mod that lets a player shrink or grow. Built for a two-person GTNH
server. It deliberately grants **no abilities** - no flight, no wall climbing,
no fall-damage negation. Only size.

Concept borrowed from [Shrink.](https://github.com/gigabit101/Shrink) by
Gigabit101 (LGPLv3). No code was copied; nothing in the modern version ports to
1.7.10, so this is an original implementation.

Status: **working**, verified in GTNH 2.9.0 Beta2 singleplayer.

## Using it

**Size Remote** - creative Tools tab, or `/give <player> shrinkgrow:size_remote`.
Right-click to open a screen with `-0.5 / -0.1 / +0.1 / +0.5`, a live readout,
Reset and Done.

**Keys** - `[` shrink, `]` grow, `\` reset; hold sneak for a coarser step.
Rebindable in Options -> Controls under "Shrink & Grow".

**Commands** - `/scale <number|reset|info> [player]`. Scaling another player
needs permission level 2 by default. `/scale info` dumps raw geometry (width,
height, posY, yOffset, eyeHeight, bounding box, drift) and is the fastest way to
diagnose a size bug - use it before trying to read a screenshot.

Config lives in `config/shrinkgrow.cfg`: min/max scale, step sizes, whether step
height and reach scale with you, and `cameraFudge` if the first-person camera
ever sits wrong.

Install the same jar on **both** client and server.

## Versioning and releases

The jar name comes from `git describe`, so it is only clean when HEAD sits
exactly on a tag with no uncommitted changes:

    git describe --tags        # must print just the version, e.g. 1.0.1

To cut a release:

    git add -A && git commit -m "what changed"
    git tag -a 1.0.2 -m "what changed"
    .\gradlew spotlessApply build

That produces `build/libs/shrinkgrow-1.0.2.jar`.

Building with uncommitted changes gives something like
`1.0.1-3-gabc1234-dirty` instead. That is a useful warning rather than a
failure, but never deploy a jar with `-dirty` in the name: its exact source is
not recorded anywhere, so it cannot be reproduced or rolled back.

Keep the client and server on byte-identical jars.

## Building

    .\gradlew spotlessApply build

Output in `build/libs/` - take the plain jar, not `-dev` or `-sources`.
`spotlessApply` first, or the build fails the formatting check.

Note: Gradle must be run from a normal terminal. Java NIO `Selector.open()`
fails inside some sandboxed/spawned process environments on this machine with
"Unable to establish loopback connection", which kills the Gradle daemon before
it starts. This is NOT caused by PIA or Radmin VPN - both were ruled out; a
normal interactive shell works fine with them running.

## How it works, and the 1.7.10 traps

Everything below was verified against the decompiled sources, not inferred.

**Geometry.** `Entity.setPosition` derives the box as
`minY = posY - yOffset + ySize`, and `moveEntity` derives position back as
`posY = boundingBox.minY + yOffset - ySize` - the **box is authoritative**.
`SizeApplier` therefore anchors `minY` and grows the box upward. An earlier
version called `setPosition()` to rebuild the box from `posY`; that inverts the
dependency and teleports the box whenever `posY` is briefly out of step.

`Entity.setSize` is `protected`, so it is reproduced from public fields rather
than opened up with an access transformer.

**yOffset is not constant across sides, and must not appear in any formula.**
`EntityPlayerMP` zeroes it, so server-side `posY` sits at the feet while the
client keeps 1.62. Writing a formula "in terms of yOffset" therefore produces a
*different answer on each side* - this was the cause of a real bug (see below).
Size itself is safe because width/height/box are side-symmetric.

**Eye height is side-dependent and must be measured, not assumed.**
Values measured in GTNH 2.9 RC1 with `/scaleclient` and `/scale info`:

| | client | server |
| --- | --- | --- |
| `yOffset` | 1.62 | 0 (EntityPlayerMP zeroes it) |
| `getDefaultEyeHeight()` | **0.12** | **1.62** |
| `posY` sits at | feet + 1.62 | the feet |
| vanilla ray origin | `posY + (eyeHeight - g)` | `posY + eyeHeight` |

Both the constants *and* the formula differ per side, so eye height is solved
separately for each, targeting the geometry directly:

    target = 1.62 * scale                        // eye height above the feet
    client: eyeHeight = target - yOffset + g
    server: eyeHeight = target - yOffset

At scale 1 each branch returns that side's own stock value (0.12 and 1.62),
so normal size is provably untouched - which is the property to check first if
this ever needs changing again.

Two earlier attempts failed here, both by assuming instead of measuring.
`(yOffset + g) * scale - yOffset` was right only on the server. Replacing it
with `g * scale` fixed the server-correct case and left the client ray at very
nearly full height, because the client's `g` is 0.12 rather than the 1.62 that
had been measured on the server - so shrinking barely moved it, and the ray
targeted blocks roughly a block above the crosshair.

**Camera.** `EntityRenderer.orientCamera` computes the camera from `posY` and a
hardcoded `1.62F` and ignores `eyeHeight` entirely, so writing `eyeHeight` does
nothing to the view. `CameraHandler` instead shifts `posY` for the duration of
the render tick and restores it after - the technique Morph uses. Note Morph's
own formula implies 1.62 + 0.12; the camera really sits 1.62 above the feet, and
this uses the verified value.

**Model rendering.** `RenderPlayerEvent.Pre` fires as the *first statement* of
`RenderPlayer.doRender`, before any GL translation, so a bare `glScalef` there
scales the world-space translate that follows and flings the model away by
`(scale - 1) x` its position relative to the camera. `ClientEventHandler`
computes vanilla's translate itself and pre-compensates.

## Known cosmetic issues

- **F3+B hitbox overlay is wrong while scaled.** `renderDebugBoundingBox` builds
  its box from `posY` rather than from `entity.boundingBox`, so it picks up the
  render-tick camera shift and draws offset by `1.62 x (1 - scale)`. The real
  collision box is correct - proven by `/scale info` (drift 0.000, feet exactly
  on the ground) and by walking through a 1-block tunnel without suffocating.
  Fixing it properly means a Mixin on `orientCamera` to confine the shift;
  GTNH ships UniMixins, so it is available if it ever becomes worth doing.
- **Matter Manipulator region select is wrong while scaled, and the bug is in
  MM, not here.** `MMUtils.getLookingAtLocation` does
  `getPosition()` (`posY + (eyeHeight - g)`) and then, server-side only, adds
  `getEyeHeight()` on top. Those cancel only when `eyeHeight == g`, i.e. only
  for an unscaled player. With scale 0.5 the server's ray starts at the feet,
  0.81 too low. The upstream fix is one token - add `getDefaultEyeHeight()`
  rather than `getEyeHeight()`, which is identical for every normal player.
  It cannot be fixed from this mod: MM's server path would need
  `eyeHeight = 1.215` where the vanilla path needs `0.81`, and one field cannot
  be both. Options are an upstream report or a local Mixin `@Redirect` on that
  single call site.
- Nameplates above a scaled player render scaled too.
- Shadows stay normal size regardless of scale.

## GTNH notes

The GTNH team has twice rejected Morph, explicitly including the hitbox-scaling
mechanic (it makes cheesing mobs easier). This mod is unofficial and unsupported
by them by design - keep it to private servers.
