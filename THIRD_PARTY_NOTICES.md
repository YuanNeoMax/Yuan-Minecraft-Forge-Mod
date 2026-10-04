# Third-Party Notices

This mod (`yuan`) includes or adapts content from the following third parties.
Each item remains the property of its respective owner; this project does not
re-license them. Please comply with each owner's terms.

## Minecraft / Mojang

- The shader pipeline runs on Minecraft's vanilla shader framework, and
  `yuan_style.json` uses the vanilla `blit` vertex program. Game assets and the
  shader framework are owned by Mojang/Microsoft and used under their
  modding/asset guidelines.

## paper-design/shaders (晶格 / Voronoi rendering)

The "晶格" (lattice / Voronoi) weapon render style is ported from
`@paper-design/shaders`, upstream https://github.com/paper-design/shaders
(original algorithm reference: https://www.shadertoy.com/view/ldl3W8).

- `assets/yuan/shaders/core/voronoi_item.fsh` — two-pass Voronoi cell edges,
  noise displacement, 5-color gradient, inner glow and cell gaps.
- `assets/yuan/textures/effect/voronoi_noise.png` — original random-noise texture.

Port modifications for Minecraft 1.20.1 `#version 150`: explicit
`VoronoiColor0..4` color uniforms, `SpriteBounds` blade UV space, view-following
(`ViewYaw`/`ViewPitch`) and X/Y offset config; the algorithm itself is unchanged.
Licensed under **Apache License 2.0**
(https://www.apache.org/licenses/LICENSE-2.0).

## ArcaneVortex / 秘法涡流 (cosmic rendering)

- `assets/yuan/textures/item/cosmic_0.png` … `cosmic_9.png` — texture images
  from the third-party ArcaneVortex mod (Forge 1.20.1).
- `assets/yuan/shaders/core/cosmic_neo.fsh` — shader copied from the same mod
  (the "Van Sh" cosmic effect).

ArcaneVortex / 秘法涡流 is originally released by Bilibili UP主「洛谔谔」
(UID 3546888156481679, see https://space.bilibili.com/3546888156481679).
These files are used unchanged for the cosmic starfield rendering of the Yuan
God Sword. ArcaneVortex is a closed-source third-party project; the materials
above are included for reference and are not licensed under this project's MIT
license. All rights remain with the original author.

## 超级时停 / megatimestop (sounds & post-processing)

- `assets/yuan/sounds/stop.ogg`, `assets/yuan/sounds/start.ogg` — sound files
  copied from the reference resources of the third-party mod `超级时停`
  (megatimestop).
- `assets/yuan/textures/item/white.png` — plain white texture copied from the
  same mod's reference resources.
- `assets/minecraft/shaders/post/the_world.json` (the gray-screen "The World"
  post chain) and the `motion_blur` / `rewind` program shaders are ported from
  that mod's reference resources; `yuan_world_style.json` / `yuan_style.*` are
  custom variants built on top of those programs.

All rights remain with the original author.

## Silk WebGL shader (silk rendering)

- `assets/yuan/shaders/core/silk_item.*` — the silk weapon shader is ported
  from the "Silk" WebGL shader project. Credit and original license belong to
  its authors; this port keeps the original intent with mod-specific
  integration and is included for reference.

## Photon / 光子 (VFX library, bundled as a nested mod)

The space-slash streak FX run on the Photon VFX library by Low-Drag-MC
(https://github.com/Low-Drag-MC/Photon, editor on Modrinth/CurseForge).

- This mod bundles the **unmodified** `photon-forge-1.20.1-1.1.17` jar as a
  Forge Jar-In-Jar nested mod (`META-INF/jars/`); Photon in turn nests LDLib
  and MixinExtras. No Photon class or resource is extracted, modified or
  re-licensed.
- Photon is licensed **CC BY-NC-SA 4.0**
  (https://creativecommons.org/licenses/by-nc-sa/4.0/): share and adapt only
  non-commercially, with attribution, under the same license. This project's
  MIT license therefore does **not** cover the bundled Photon jar.

## Live2D — see the separate YuanLive2D project

The (optional) Live2D companion mod and its bundled SDK/model live in a
separate repository (`YuanLive2D`); this mod does not bundle Live2D content.

---

If you are the owner of any material listed here and believe its inclusion is
problematic, please open an issue and it will be removed promptly.