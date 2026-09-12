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

### Fluix Aggregator

- **The Fluix Aggregator is back**, with the recipes it always had: fluix out of quartz, redstone and charged
  certus quartz, a resonating crystal, a speculation core, and the two halves of steelmaking. A machine a world
  already holds is read as it was - what is in its slots, what is in its power buffer, how far along its work
  was, which faces it lets items through, and whether it was exporting.
- **Acceleration Cards go in a column of eight slots** beside the window, one card to a slot, the way every
  machine in AE2 takes them - rather than a stack of eight in a single slot. How many fit is a config line
  (`upgrades.cards`), and the cards are registered with AE2, so a card's own tooltip now lists this machine
  among the places it goes.
- **Auto-export is AE2's own button**, in the column of settings down the left edge, instead of a switch of the
  mod's own; it hands what the machine made to whatever stands against a face that lets items out, as before.
- The little map of the machine's six faces stays: click a face to have it take items in, let them out, do
  both, or nothing. AE2 has nothing of the kind to borrow, so this one is drawn from the old mod's picture.
- **The recipes have a screen in HEI**, three slots in and one out, and the machine is listed as what makes
  them.

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
