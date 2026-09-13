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

- **A freshly placed machine is open on every face**, taking items in and letting them out, where the old mod
  started it closed. A closed machine is invisible to everything standing beside it: a hopper moves nothing, an
  ME Interface and a Preemptive Assembly Unit read it as "Nothing" and cannot push into it. The map of faces is
  still there to narrow that down, and a machine a world already holds keeps the map it was given.

- **A machine's arrow opens its recipes in HEI.** Clicking the arrow between the slots shows what that machine
  makes, and the arrow says so when hovered - in HEI's own words, so it is already translated. Only with a
  recipe viewer installed.

### ME Level Maintainer Terminal

- **A window that lists every ME Level Maintainer on the network**, each with its five rows under it, all of
  them editable from where you stand: what a row keeps, how much it orders at a time, and whether it is
  switched on. The old mod had no such thing; the idea is ME Requester's terminal
  (`AlmostReliable/merequester`), which does the same for its requesters.
- **A row here answers the gestures it answers on the machine's own window**: the wheel over the slot steps
  how much to keep, Ctrl halves or doubles it, a middle click types it, and anything dropped in - by hand or
  dragged out of HEI, item or fluid - becomes what that row watches.
- **Click a number to type it.** How much a row orders at a time is drawn where it stands, and a click turns
  that into a field with a tick beside it; Enter or the tick saves it, and going anywhere else gives up on it.
- **Two search boxes**: one asks the rows what they keep, in the grammar the terminals use, and dims every row
  that does not answer while marking the ones that do; the other asks the machines their name. Both are
  remembered while the window is closed.
- **The button beside a machine's name marks it in the world** and turns the player towards it, the way a
  Pattern Access Terminal points at an interface.
- **It is a part on a cable and a face of the wireless terminal.** Crafting the part into a wireless terminal
  unlocks the mode, which then has a button in the terminal and a key of its own, exactly like AE2's own
  terminals. Both can be taken away in `config/lazy_ae2.cfg` (`levelMaintainer.terminal`).

### ME Level Maintainer

- **The ME Level Maintainer is back**, with five rows: each names something the network should always have
  some of, how much to keep, and how much to order at a time. It watches the network rather than counting for
  itself, so it only acts when an amount actually moves, and it slows down while there is nothing to do.
- **A row stands for anything the network can hold**, not only an item. A fluid, or anything an addon
  registers a key type for, is kept to a level the same way - drag it into the row from a recipe screen or
  drop it in from the network.
- **No result slots.** The old mod caught what a craft made in five slots of its own and put it into the
  network from there, which meant a full slot could stall the row and the items could be lost on a break. What
  the job makes now stays in network storage, which is where it was wanted. Whatever the old slots still held
  is handed to the network the first time the machine ticks.
- **A whole batch is ordered at a time.** The old mod ordered exactly what was missing, so taking three items
  out of the network started a crafting job for three. A row now orders its batch the moment the level drops
  below what it keeps, and the level may end up a little over.
- **A row can be switched off** with the button beside it, and keeps everything it says while it is: what to
  keep, how much to order. Whatever it had ordered is called off with it.
- **The slot itself says how much to keep**, drawn on it in that thing's own units - buckets for a fluid -
  the way a level emitter wears its threshold, and the exact number is in the row's tooltip, since a slot has
  room for a rounded one only. The wheel over the slot steps it and a middle click types it - the same
  gestures, and the same steps, that every filter slot in AE2 answers to. The field beside it is that step, which is also how much the row orders at
  once; the tick beside the field lights up as soon as the number differs from the machine's, and saves it -
  as does Enter.

### Preemptive Assembly Unit

- **The Preemptive Assembly Unit is back.** It offers the network the processing patterns it holds and hands
  the ingredients to whatever machine stands against a face that lets items out. What that machine gives back
  can be piped straight into the unit, which puts it into network storage - no import bus needed.
- **It keeps taking work while it is busy**, which is the whole point of it: an interface tells the crafting
  CPU to wait until what it holds has gone, while this one takes one more pattern as long as its buffer has
  room, so a machine that queues a hundred operations is filled rather than fed one at a time. The old mod
  did that by reaching into the CPU's private task list; here the CPU keeps pushing of its own accord, at the
  rate its co-processors allow.
- **Nine pattern slots, and a row more for each Pattern Expansion Card**, up to 36, exactly as an ME Interface
  does it - the old mod had nine and nothing else. A card will not come out from under its patterns, so none
  are ever spilled, and how many fit is a config line (`upgrades.cards`).
- **Both pattern terminals know it**: the Pattern Access Terminal lists it beside the interfaces, with its
  patterns editable from there, and a pattern encoded in a Pattern Terminal can be sent straight to it.
- It offers **processing patterns** only: a crafting pattern belongs in an interface, where the Molecular
  Assembler beside it reads the pattern itself.
- In a terminal it is **named after the machine it stands against**, with that machine's picture beside it,
  exactly as an ME Interface names itself - and a Quartz Cutting Knife renames it, which every machine of this
  mod now takes. The name a player gives it wins over the machine's own, and the window wears it too. **A
  picture can be chosen there as well**, in the same knife's window, exactly as an interface takes one.
- A unit a world already holds keeps its patterns, both buffers, its faces and its place on the network.

### Crystal Energizer

- **The Crystal Energizer is back**, charging certus quartz crystals without a network, as many at a time as
  its cards allow. A machine a world already holds keeps its slots, its power, its work and its faces.
- **Each recipe carries its own price** - 12 000 FE for a charged certus crystal - rather than the machine
  charging one flat rate for everything, and HEI's screen says what that price is. Switching the crystal in
  the slot for one that costs something else now re-prices the work; the old mod kept charging the first
  price until a card was moved.
- It takes the same eight Acceleration Cards in a column of their own, the same auto-export button and the
  same map of faces as the other machines.

### ME Circuit Etcher

- **The ME Circuit Etcher is back**, pressing a processor out of gold, purified certus quartz, diamond, a
  resonating crystal or a 64x speculation core, between redstone above and silicon below. A machine a world
  already holds keeps its slots, its power, its work and its faces.
- **Each slot takes only what belongs in it**: the two pressing agents cannot be swapped, and the material
  cannot be dropped in on top of them. Hoppers and pipes are held to the same rule.
- It takes the same eight Acceleration Cards in a column of their own, the same auto-export button and the
  same map of faces as the other machines, and its recipes have a screen in HEI. The presses still close over
  the first half of the work and the arrow fills over the second, as they always did.

### Pulse Centrifuge

- **The Pulse Centrifuge is back**, purifying certus, nether quartz and fluix crystals two at a time, and
  grinding sky stone, ender pearls and wheat as it always did. A machine a world already holds keeps its
  slots, its power, its work and its faces.
- It takes the same eight Acceleration Cards in a column of their own, the same auto-export button and the
  same map of faces as the Fluix Aggregator, and its recipes have a screen in HEI.

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
