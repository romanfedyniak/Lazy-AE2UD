# Lazy AE2 Unofficial Deconstructed

*AE2 for people who hate microcrafting*

Machines that automate [AE2 Unofficial Deconstructed](https://github.com/romanfedyniak/AE2UD)'s in-world crafting,
on Minecraft 1.12.2.

![](show.png)

![](ma_chamber.png)

This is a rewrite in Java of [Lazy AE2](https://github.com/phantamanta44/Lazy-AE2) by phantamanta44. LibNine, the
library the original was built on, is gone, so it no longer has to be installed. The mod is written against AE2UD
and does not run with any other AE2 build.

See [CHANGES.md](CHANGES.md) for what it adds and how it differs from the mod it continues.

## What it adds

* **Fluix Aggregator** - performs the in-world fluix crystal crafting operation.
* **Pulse Centrifuge** - performs the in-world crystal seed growing operation.
* **ME Circuit Etcher** - etches circuits without pressing the components.
* **Crystal Energizer** - charges certus quartz more efficiently than the AE2 charger.
* **Preemptive Assembly Unit** - an ME interface that dispatches crafting operations eagerly, filling a machine
  rather than feeding it one recipe at a time.
* **ME Level Maintainer** - keeps a quantity of anything the network stores in stock, by requesting autocrafting
  when it runs low.
* **Mass Assembly Chamber** - a really big multi-block molecular assembler that goes fast and holds lots of
  patterns.

Every machine can be switched off in `config/lazy_ae2.cfg`, which takes its recipes with it.

## Requirements

* Minecraft 1.12.2 with Minecraft Forge.
* AE2 Unofficial Deconstructed 1.6.0 or newer, with what it requires.

## Building

Gradle runs on Java 25: `./gradlew build`. AE2UD is fetched from [JitPack](https://jitpack.io), so nothing has to be
built first.

## License

The code is licensed under the MIT License with the "Good, not Evil" clause; see [LICENSE.md](LICENSE.md). The
textures and models come from Lazy AE2 by phantamanta44, under the same license.
