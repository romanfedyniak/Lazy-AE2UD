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
