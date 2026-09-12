# Changelog

All notable Lazy AE2 Unofficial Deconstructed changes are grouped by the version in which they first appeared.

## Important compatibility notice

- Lazy AE2 Unofficial Deconstructed runs only with AE2 Unofficial Deconstructed, not with standard AE2 or any other
  AE2 build.
- A world saved with Lazy AE2 1.1.26 is read: the blocks keep their names, and what each machine held is carried
  over.
- Back up the world before installing or updating the mod.

## Unreleased

- **Rewritten from scratch in Java.** LibNine, the library the original was built on, is gone, so it no longer has
  to be installed. Every window, tile and recipe is written against AE2 Unofficial Deconstructed instead.
- **Every machine can be switched off**, in `config/lazy_ae2.cfg`, which takes its blocks and their recipes with
  it. The file keeps its old name and a server that had tuned the old numbers keeps them: the values under
  `general.processing`, `general.networkdevices` and `general.massassembler` are read once into the new sections,
  one per machine, and the old sections are then dropped.
- Only the English and Ukrainian translations ship. The partial translations into other languages are removed.

### Materials

- **All fifteen materials are back**, in the order the old mod registered them, so a saved world keeps what it
  holds: the metadata of a stack is what identifies it. They sit in the mod's own creative tab, as before.
- The recipes that need none of this mod's machines come with them: the crafting-table ones, the Inscriber press
  that plates an iron ingot, and the furnace smelt that turns a plated ingot into fluix steel. Everything else a
  material is made in ships with the machine that makes it.
- **Coal Dust can be switched off**, with `materials.coalDust`, for a pack that already has one. Nothing of ours is
  then registered as `dustCoal` and the item is no longer offered, while a stack already in a world keeps working.
  Every recipe that takes coal dust takes it by ore dictionary, so another mod's does just as well. Note that our
  Coal Dust has no recipe of its own in either mod: it is made in AE2's grindstone, which builds that recipe itself
  out of the ore dictionary, so switching ours off in a pack with no other coal dust leaves fluix steel unmakeable.
