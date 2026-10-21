# Sodium/Embeddium LeafCulling

A mod that optimizes leaf rendering while preserving their appearance for a slight FPS improvement.
An unofficial fork of [Sodium Leaf Culling](https://github.com/txnimc/SodiumLeafCulling) by Txni.

## Features

- Three culling modes:
  - **Hollow:** Renders only the outer layers of leaves, similar to CullLeaves. The best performance, but terrible visuals.
  - **Solid:** Renders leaves as solid when they are completely enclosed on all six sides. The best visuals, with decent performance.
  - **Solid Aggressive:** Renders leaves as solid when they are completely enclosed horizontally. Better performance than Solid, at the cost of visual quality.
- **Distance-based culling:** Keeps the selected mode for nearby leaves and renders all distant leaves as solid.
- **Integrated settings:** Configuration is available through the Sodium/Embeddium settings screen on every supported version.
- **Multiple loaders and Minecraft versions:** Synchronized releases keep every supported version on the same mod release, so new features remain available on older Minecraft versions too.

## Requirements

Use dependency versions compatible with your Minecraft version and this mod.

- **Fabric:** Sodium and Fabric API.
- **NeoForge:** Sodium or Embeddium, depending on the Minecraft version.
- **Forge:** Embeddium.

## Why was a fork necessary?

To make SodiumLeafCulling available on a wider range of Minecraft versions and mod loaders, continue support, and fix bugs.

## Supported Versions

| Minecraft | Fabric | Forge | NeoForge |
| --- | :---: | :---: | :---: |
| 1.16.3–1.16.4 | Yes | — | — |
| 1.16.5 | Yes | Yes | — |
| 1.17–1.17.1 | Yes | — | — |
| 1.18–1.18.1 | Yes | — | — |
| 1.18.2 | Yes | Yes | — |
| 1.19–1.19.1 | Yes | — | — |
| 1.19.2 | Yes | Yes | — |
| 1.19.3–1.20 | Yes | — | — |
| 1.20.1 | Yes | Yes | — |
| 1.20.2 | Yes | Yes | Yes |
| 1.20.3 | Yes | — | Yes |
| 1.20.4 | Yes | — | Yes |
| 1.20.5–1.20.6 | Yes | — | Yes |
| 1.21 | Yes | — | Yes |
| 1.21.1–1.21.8 | Yes | — | Yes |
| 1.21.9 | Yes | — | — |
| 1.21.10–1.21.11 | Yes | — | Yes |
| 26.1, 26.1.1, 26.1.2, 26.2, 26.3 | Yes | — | Yes |
| 26.2-rc-1, 26.2-rc-2, 26.3-rc-1, 26.3-rc-2 | Yes | — | — |

## Credits

- [Txni](https://github.com/orgs/txnimc): Author of the original [SodiumLeafCulling](https://modrinth.com/mod/sodiumleafculling) and developer of its algorithm.
  - Thank you to Txni for creating SodiumLeafCulling and its algorithm and making them open source.
- [Stonecutter](https://stonecutter.kikugie.dev/)
  - The Stonecutter Gradle plugin makes supporting multiple Minecraft versions much easier.

## License

This project is licensed under the [Blue Oak Model License 1.0.0](LICENSE.md).
