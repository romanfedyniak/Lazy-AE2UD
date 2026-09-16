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

### Mass Assembly Chamber

- **The chamber's blocks are back** - frame, vent, controller, IO port, pattern module and co-processing
  module - with the old mod's recipes, and a chamber a world holds keeps its blocks and every pattern in its
  modules. A pattern module takes **crafting patterns only**, through the IO port as well - the old
  mod let a processing pattern in and then never used it. **The same pattern goes in once**: a second copy of
  one already anywhere in the chamber is refused, since it would only take a slot.
- **The chamber crafts as many things at once as it has slots**: one of its own, plus what its co-processing
  modules add. Every craft takes 10 ticks and 100 AE, spread over those ticks - exactly a Molecular Assembler's
  numbers, so a chamber is that many assemblers in one box. Short of power a craft slows down, as an
  assembler's does, and the power there is goes to the oldest crafts first, so something still finishes. The
  old mod pooled work across the whole chamber instead, which let one craft finish instantly with enough
  co-processors and made the progress bar a pool rather than anything a craft was doing.
- **A crafting CPU hands the chamber everything its free slots take in one go**, through AE2's new batch push
  (`ICraftingMedium.maxCopies`), and a batch finishes, and is delivered, as one. A chamber of ten thousand slots
  costs the server one call and one delivery per batch rather than ten thousand of each. The CPU still spends
  one of its operations per craft, so filling a large chamber quickly still takes co-processors.
- **What a craft makes is worked out when it starts**, so the chamber keeps no ingredients - the old mod's
  buffer of every queued job's nine inputs is gone. Each pattern is offered to the network once, however many
  modules hold it.
- **What the network will not take stays in the chamber and keeps its slots**, so a chamber with nowhere to put
  its results stops taking work until there is; it offers them again every second.
- **Taking a chamber apart does not throw away what it took**: it finishes that work and delivers it, and only
  takes nothing new. **Breaking the controller** hands everything it was crafting to the network at once,
  finished or not, so a job waiting for it carries on; whatever the network refuses drops.
- **Both pattern terminals know the chamber**: the Pattern Access Terminal lists it under the controller's name
  and picture, with every module's patterns in one entry, four rows a module, editable from there and pointing
  at the controller; and a pattern encoded in a Pattern Terminal can be sent straight to it. It takes crafting
  patterns only, and only while it is assembled.
- **The Network Tool shows what a chamber draws while it crafts**, not only its idle 3 AE/t, through AE2's
  `IPowerUsageReporter` - so a network losing power to a busy chamber says where it goes.
- **The config is `ticksPerJob` (10) and `energyPerJob` (100)** now. `jobQueueSize`, `workPerJob`,
  `workPerTickBase`, `workPerTickUpgrade`, `energyPerWorkBase` and `energyPerWorkUpgrade` described the pooled
  work and are removed, so a value tuned in them is not carried over. A chamber the old mod saved keeps its
  queued jobs, each starting over as one craft, and everything in its output buffer is delivered.
- **Co-processing modules come in five tiers**, 1x, 4x, 16x, 64x and 256x, each made with AE2's co-processing
  unit of the same size in place of the plain one. A tier's worth is how many more crafts the chamber runs at
  once, and its tooltip says so. The old mod had one module, and a chamber it saved keeps it as 1x.
- **A failed assembly says why**, in chat: which block at which coordinates is the wrong one, that the box is
  too small or too large, that the controller stands on an edge, that a block belongs to another chamber, or
  that there is no pattern module inside. The old mod said only that assembly failed. A chamber that
  assembles says how large it is and how many crafts it runs at once.
- **Assembly is still a click on the controller**, with an empty hand; once assembled, a click on any block of
  the chamber opens its window, and a sneaking click on the controller takes the chamber apart. The controller's
  tooltip says so, and clicking any other block of a chamber that is not assembled says to click the controller
  rather than doing nothing. A click with a block in hand places the block, so a chamber can be built against
  its own walls.
- **The window lists every pattern of every module** under a heading of its own, four rows a module, on the
  Pattern Access Terminal's frame and scrolling the same way, as tall as the terminal style allows - the old
  mod showed one module a page. Patterns go in and out with the same clicks as in that terminal, and follow
  the module's rules. **The search reads what a pattern makes**, in the terminals' grammar: a module with
  nothing that matches is left out and what matches is marked. **Two bars** say how many crafts are running
  out of how many the chamber can, and how many pattern slots are filled; the first one's tooltip gives the
  crafts a second and the power drawn. The old mod's picture of the job in progress is gone: out of hundreds
  of crafts at once, it showed one.
- **How large a chamber may be is a config line**, per axis, walls included - `massAssembler.maxSizeX`, `Y`
  and `Z`, 8 by default, which is the 8 × 8 × 8 the old mod allowed. **`massAssembler.requireSingleChunk`**
  keeps a chamber inside one chunk, as AE2's own `craftingCPU.requireSingleChunk` does for crafting CPUs. The
  controller's tooltip gives the size in force, and on a server it is the server's.
- A chamber the old mod saved assembled never wrote down where its walls are, so it is **assembled again when
  its controller loads**, and taken apart if its walls no longer stand. A chamber is checked the same way every
  time it loads, so one that lost a wall while nobody was near is not left assembled.

### ME Level Maintainer Terminal

- **A window that lists every ME Level Maintainer on the network**, each with its five rows under it, all of
  them editable from where you stand: what a row keeps, how much it orders at a time, and whether it is
  switched on. The old mod had no such thing; the idea is ME Requester's terminal
  (`AlmostReliable/merequester`), which does the same for its requesters.
- **A row here answers the gestures it answers on the machine's own window**: the wheel over the slot steps
  how much to keep, Ctrl halves or doubles it, a middle click types it, and anything dropped in - by hand or
  dragged out of HEI, item or fluid - becomes what that row watches.
- **A container dropped from HEI follows the same rule as everywhere else**: dropped with the left button
  the row watches what it holds, with the right the container itself. The terminal used to take the
  container whichever button ended the drag, so a row could not be pointed at a fluid from a recipe at all.
- **Click a number to type it.** How much a row orders at a time is drawn where it stands, and a click turns
  that into a field with a tick beside it; Enter or the tick saves it, and going anywhere else gives up on it.
- **Two search boxes**: one asks the rows what they keep, in the grammar the terminals use, and dims every row
  that does not answer while marking the ones that do; the other asks the machines their name. Both are
  remembered while the window is closed.
- **A row wears what it is doing here too**, as a mark in the corner of its item in the colour the
  machine's own window gives it, with the words in the item's tooltip.
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
- **A row's tooltip says what a click on it would do**, in AE2's own wording: that a middle click types how
  much to keep, and with something in hand, which button would set the row to it. Both windows hand their
  tooltips to AE2 to finish rather than drawing them past it.
- **Every row says what it is doing.** The strip under a row is green while the network holds as much as
  the row keeps, blue while a plan is being worked out, yellow while the job runs, orange while every
  crafting processor is busy, and red when nothing on the network makes that thing or the network turned the
  job down. Hovering the strip, or the row's item, says which in words. GTNH's Level Maintainer marks its
  rows the same way (`GTNewHorizons/AE2FluidCraft-Rework`).
- **A maintainer with no channel does nothing at all.** AE2 asks a machine to work whether or not it has a
  channel, and this one went on planning and ordering crafts without one.
- **A row the network cannot fill stops asking.** The machine looks before it plans: with nothing on the
  network able to make that thing, or with every crafting processor busy, no plan is worked out at all. A
  plan that came back a simulation, or a job the network turned down, puts that row on a wait
  (`levelMaintainer.retryTicks`, 200 ticks) instead of being worked out again a few ticks later - one row
  nobody could fill used to keep its machine running at full speed for as long as it stood. Editing the row,
  a change in what it watches, or opening a window that shows it ends the wait at once. Looking before
  planning is how GTNH's Level Maintainer does it (`GTNewHorizons/AE2FluidCraft-Rework`).

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
