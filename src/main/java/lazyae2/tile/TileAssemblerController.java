/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.tile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.netty.buffer.ByteBuf;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.EmptyHandler;

import lazyae2.block.BlockAssembler;
import lazyae2.core.LazyAE2Config;
import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingMedium;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.networking.crafting.IPatternContainer;
import appeng.api.networking.crafting.MachineIdentity;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.energy.IPowerUsageReporter;
import appeng.api.networking.events.MENetworkChannelsChanged;
import appeng.api.networking.events.MENetworkCraftingPatternChange;
import appeng.api.networking.events.MENetworkEventSubscribe;
import appeng.api.networking.events.MENetworkPowerStatusChange;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.MEStorage;
import appeng.api.util.DimensionalCoord;
import appeng.core.MultiblockLimits;
import appeng.hooks.TickHandler;
import appeng.me.GridAccessException;
import appeng.me.helpers.MachineSource;
import appeng.me.helpers.PowerUsageMeter;
import appeng.tile.grid.AENetworkTile;
import appeng.util.Platform;
import appeng.util.inv.WrapperChainedItemHandler;

/**
 * The controller of a Mass Assembly Chamber: the block a player clicks to assemble the chamber around it, and
 * the crafting medium the network hands the chamber's work to.
 * <p>
 * The chamber runs as many crafts at once as it has slots, each taking as long and as much power as a
 * Molecular Assembler's. A crafting CPU hands it all the crafts its free slots take in one call, and they
 * finish, and are delivered, as one batch.
 */
public final class TileAssemblerController extends AENetworkTile
        implements BlockAssembler.IAssemblerBlock, ICraftingProvider, ICraftingMedium, IGridTickable,
        IPowerUsageReporter, IPatternContainer {

    private static final GenericStack[] NO_EXTRAS = new GenericStack[0];

    private final AssemblerWork work = new AssemblerWork();
    private final IActionSource source = new MachineSource(this);
    private final PowerUsageMeter powerUsage = new PowerUsageMeter();

    private boolean assembled;
    @Nullable
    private BlockPos min;
    @Nullable
    private BlockPos max;
    private int parallel;
    private List<BlockPos> patternModules = Collections.emptyList();

    /** Set by a chamber the old mod saved assembled, which never wrote down where its walls are. */
    private boolean legacyAssembled;

    public TileAssemblerController() {
        this.getProxy().setFlags(GridFlags.REQUIRE_CHANNEL);
        this.getProxy().setIdlePowerUsage(LazyAE2Config.instance().getMassAssemblerIdlePower());
        this.getProxy().setVisualRepresentation(BlockAssembler.Type.CONTROLLER.newStack(1));
    }

    @Override
    public boolean isAssembled() {
        return this.assembled;
    }

    /** How many crafts the chamber runs at once, or none while it is not assembled. */
    public int getParallel() {
        return this.assembled ? this.parallel : 0;
    }

    /** How large the chamber is, walls included, or null while that is not known. */
    @Nullable
    public BlockPos getSize() {
        return this.assembled && this.min != null && this.max != null
                ? this.max.subtract(this.min).add(1, 1, 1)
                : null;
    }

    public int getPatternModuleCount() {
        return this.patternModules.size();
    }

    /**
     * A click on the controller: assembles the chamber, or takes an assembled one apart, and says which - or,
     * when it cannot be assembled, why not.
     */
    public void toggleAssembly(final EntityPlayer player) {
        if (this.assembled) {
            this.disassemble();
            player.sendMessage(BlockAssembler.message("chat.threng.assembler.disassembled", TextFormatting.YELLOW));
            return;
        }

        final AssemblerStructure.Outcome outcome = AssemblerStructure.check(this.world, this.pos);
        if (outcome.layout == null) {
            player.sendMessage(outcome.refusal);
            return;
        }
        this.assemble(outcome.layout);
        final BlockPos size = this.getSize();
        player.sendMessage(BlockAssembler.message("chat.threng.assembler.assembled", TextFormatting.GREEN,
                size.getX(), size.getY(), size.getZ(), this.parallel, this.patternModules.size()));
    }

    private void assemble(final AssemblerStructure.Layout layout) {
        this.min = layout.min;
        this.max = layout.max;
        this.parallel = layout.parallel;
        this.patternModules = new ArrayList<>(layout.patternModules);
        this.assembled = true;
        this.legacyAssembled = false;

        for (final BlockPos at : BlockPos.getAllInBox(layout.min, layout.max)) {
            final TileEntity tile = this.world.getTileEntity(at);
            if (tile instanceof TileAssemblerPart) {
                ((TileAssemblerPart) tile).setControllerPos(this.pos);
            }
        }
        this.onPatternsChanged();
        this.saveChanges();
        this.markForUpdate();
    }

    /**
     * Takes the chamber apart. What it already took is finished all the same; it only takes nothing new.
     * <p>
     * A block in a chunk that is not loaded still points here and still looks assembled until it is assembled
     * again or broken; nothing it does depends on that, since a controller that is not assembled offers no
     * patterns and takes no work.
     */
    public void disassemble() {
        if (!this.assembled && !this.legacyAssembled) {
            return;
        }
        this.assembled = false;
        this.legacyAssembled = false;

        if (this.world != null) {
            // An old chamber never said where its walls are, so every block that could be one is asked
            final MultiblockLimits.Limit limit = MultiblockLimits.get(AssemblerStructure.LIMIT);
            final BlockPos from = this.min != null ? this.min
                    : this.pos.add(1 - limit.getX(), 1 - limit.getY(), 1 - limit.getZ());
            final BlockPos to = this.max != null ? this.max
                    : this.pos.add(limit.getX() - 1, limit.getY() - 1, limit.getZ() - 1);
            for (final BlockPos at : BlockPos.getAllInBox(from, to)) {
                if (!this.world.isBlockLoaded(at)) {
                    continue;
                }
                final TileEntity tile = this.world.getTileEntity(at);
                if (tile instanceof TileAssemblerPart && this.pos.equals(((TileAssemblerPart) tile).getControllerPos())) {
                    ((TileAssemblerPart) tile).setControllerPos(null);
                }
            }
        }
        this.min = null;
        this.max = null;
        this.parallel = 0;
        this.patternModules = Collections.emptyList();
        this.onPatternsChanged();
        this.saveChanges();
        this.markForUpdate();
    }

    /** Called when a pattern module of this chamber changes, and when the chamber is assembled or taken apart. */
    void onPatternsChanged() {
        if (this.world == null || this.world.isRemote) {
            return;
        }
        try {
            this.getProxy().getGrid().postEvent(new MENetworkCraftingPatternChange(this, this.getProxy().getNode()));
        } catch (final GridAccessException ignored) {
            // no grid to tell; it asks for the patterns itself once there is one
        }
    }

    /**
     * The controller is being broken: everything the chamber was crafting goes to the network at once, finished
     * or not, while the controller is still on it - a crafting job waiting for it takes it and carries on. What
     * the network will not take drops.
     */
    public void onBroken(final List<ItemStack> drops) {
        for (final GenericStack output : this.work.takeAll()) {
            long left = output.amount() - this.store(output.what(), output.amount());
            if (left <= 0 || !(output.what() instanceof AEItemKey)) {
                continue;
            }
            final AEItemKey item = (AEItemKey) output.what();
            while (left > 0) {
                final int count = (int) Math.min(left, item.getMaxStackSize());
                drops.add(item.toStack(count));
                left -= count;
            }
        }
        this.disassemble();
    }

    private long store(final AEKey what, final long amount) {
        try {
            final MEStorage storage = this.getProxy().getStorage().getInventory();
            return Platform.poweredInsert(this.getProxy().getEnergy(), storage, what, amount, this.source,
                    Actionable.MODULATE);
        } catch (final GridAccessException ignored) {
            return 0;
        }
    }

    /** How many crafts of each thing are being made, and how many are done and wait for room. */
    public void countCrafts(final Map<AEKey, Long> working, final Map<AEKey, Long> waiting) {
        this.work.countCrafts(working, waiting);
    }

    /** How many of the chamber's slots its work holds. */
    public int getBusy() {
        return this.work.getBusy();
    }

    /** The chamber's pattern modules that are loaded, in the order they were found; none while it is not assembled. */
    public List<TileAssemblerPatterns> getPatternModules() {
        if (!this.assembled) {
            return Collections.emptyList();
        }
        final List<TileAssemblerPatterns> modules = new ArrayList<>(this.patternModules.size());
        for (final BlockPos at : this.patternModules) {
            if (!this.world.isBlockLoaded(at)) {
                continue;
            }
            final TileEntity tile = this.world.getTileEntity(at);
            if (tile instanceof TileAssemblerPatterns) {
                modules.add((TileAssemblerPatterns) tile);
            }
        }
        return modules;
    }

    /** Every pattern slot of the chamber, module after module, or null while it is not assembled. */
    @Nullable
    public IItemHandler getAllPatterns() {
        if (!this.assembled) {
            return null;
        }
        final List<IItemHandler> handlers = new ArrayList<>(this.patternModules.size());
        for (final TileAssemblerPatterns module : this.getPatternModules()) {
            handlers.add(module.getPatterns());
        }
        return new WrapperChainedItemHandler(handlers.toArray(new IItemHandler[0]));
    }

    // ---- the network side ----------------------------------------------------------------------------

    /**
     * Every crafting pattern in the chamber's modules, each once: two modules filled before the chamber was
     * assembled can hold the same one.
     */
    @Override
    public void provideCrafting(final ICraftingProviderHelper helper) {
        final IItemHandler patterns = this.getAllPatterns();
        if (patterns == null) {
            return;
        }
        final Set<AEItemKey> offered = new HashSet<>();
        for (int slot = 0; slot < patterns.getSlots(); slot++) {
            final ItemStack stack = patterns.getStackInSlot(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof ICraftingPatternItem)
                    || !offered.add(AEItemKey.of(stack))) {
                continue;
            }
            final ICraftingPatternDetails details =
                    ((ICraftingPatternItem) stack.getItem()).getPatternForItem(stack, this.world);
            if (details != null && details.isCraftable()) {
                helper.addCraftingOption(this, details);
            }
        }
    }

    /** The free slots: how many more crafts the chamber would start right now. None once it is taken apart. */
    @Override
    public int maxCopies(final ICraftingPatternDetails details) {
        return this.assembled ? Math.max(0, this.parallel - this.work.getBusy()) : 0;
    }

    @Override
    public boolean isBusy() {
        return this.maxCopies(null) <= 0;
    }

    @Override
    public boolean pushPattern(final ICraftingPatternDetails details, final InventoryCrafting table) {
        return this.pushPattern(details, table, NO_EXTRAS, 1);
    }

    @Override
    public boolean pushPattern(final ICraftingPatternDetails details, final InventoryCrafting table,
            final GenericStack[] extraInputs) {
        return this.pushPattern(details, table, extraInputs, 1);
    }

    /**
     * Takes {@code copies} crafts of one pattern as one batch. What they make is worked out now, from the table,
     * so the ingredients themselves are not kept.
     */
    @Override
    public boolean pushPattern(final ICraftingPatternDetails details, final InventoryCrafting table,
            final GenericStack[] extraInputs, final int copies) {
        if (copies < 1 || extraInputs.length > 0 || !details.isCraftable() || copies > this.maxCopies(details)
                || !this.getProxy().isActive()) {
            return false;
        }
        final ItemStack result = details.getOutput(table, this.world);
        final AEItemKey resultKey = AEItemKey.of(result);
        if (resultKey == null) {
            return false;
        }

        final List<GenericStack> outputs = new ArrayList<>();
        outputs.add(new GenericStack(resultKey, (long) result.getCount() * copies));
        for (int slot = 0; slot < table.getSizeInventory(); slot++) {
            final ItemStack left = Platform.getRemainingItem(details, slot, table.getStackInSlot(slot), true);
            final AEItemKey leftKey = left.isEmpty() ? null : AEItemKey.of(left);
            if (leftKey != null) {
                outputs.add(new GenericStack(leftKey, (long) left.getCount() * copies));
            }
        }

        this.work.add(copies, outputs);
        this.saveChanges();
        try {
            this.getProxy().getTick().alertDevice(this.getProxy().getNode());
        } catch (final GridAccessException ignored) {
            // it was active a moment ago, so there is a grid
        }
        return true;
    }

    // ---- what the pattern terminals see -------------------------------------------------------------

    /** Listed while assembled: a chamber taken apart holds its patterns in modules nobody is using. */
    @Override
    public boolean isVisibleInTerminal() {
        return this.assembled;
    }

    /** Every module's patterns, one after another - so a terminal draws four rows for each module. */
    @Nonnull
    @Override
    public IItemHandler getTerminalPatternInventory() {
        final IItemHandler patterns = this.getAllPatterns();
        return patterns == null ? EmptyHandler.INSTANCE : patterns;
    }

    /** Crafting patterns only, the rule the modules keep themselves; a duplicate is refused there too. */
    @Override
    public boolean canAccept(@Nonnull final ItemStack pattern, @Nullable final ICraftingPatternDetails details) {
        return details != null && details.isCraftable();
    }

    @Nonnull
    @Override
    public MachineIdentity getTerminalIdentity() {
        return this.getMachineIdentity();
    }

    @Nullable
    @Override
    public DimensionalCoord getTerminalLocation() {
        return new DimensionalCoord(this);
    }

    /** Both the medium and the terminal ask, and nothing here is pretended. */
    @Override
    public boolean isFakeCrafting() {
        return false;
    }

    @Override
    public MachineIdentity getMachineIdentity() {
        return new MachineIdentity("tile.threng.big_assembler.controller.name",
                BlockAssembler.Type.CONTROLLER.newStack(1));
    }

    @Override
    public DimensionalCoord getMachineLocation() {
        return new DimensionalCoord(this);
    }

    @Override
    public TickingRequest getTickingRequest(final IGridNode node) {
        return new TickingRequest(1, 1, this.work.isEmpty(), true);
    }

    /**
     * One tick of work: power for every batch still running, oldest first, then every finished batch to the
     * network. A chamber taken apart finishes what it took all the same.
     */
    @Override
    public TickRateModulation tickingRequest(final IGridNode node, final int ticksSinceLastCall) {
        if (this.work.isEmpty()) {
            return TickRateModulation.SLEEP;
        }
        if (!this.getProxy().isActive()) {
            return TickRateModulation.SAME;
        }

        final LazyAE2Config config = LazyAE2Config.instance();
        final int ticks = config.getMassAssemblerTicksPerJob();
        final double energy = config.getMassAssemblerEnergyPerJob();
        final boolean changed;

        try {
            final IEnergyGrid grid = this.getProxy().getEnergy();
            final double wanted = this.work.powerWanted(ticks, energy);
            final double got = wanted > 0 ? grid.extractAEPower(wanted, Actionable.MODULATE, PowerMultiplier.CONFIG) : 0;
            this.powerUsage.record(this.world, got, PowerMultiplier.CONFIG);
            final boolean moved = this.work.spend(got, ticks, energy);
            changed = this.work.deliver(this::store, this.world.getTotalWorldTime()) || moved;
        } catch (final GridAccessException ignored) {
            return TickRateModulation.SAME;
        }

        if (changed) {
            this.saveChanges();
        }
        return this.work.isEmpty() ? TickRateModulation.SLEEP : TickRateModulation.SAME;
    }

    /** What the crafts took over the last second, which the Network Tool adds to the idle drain. */
    @Override
    public double getActivePowerUsage() {
        return this.powerUsage.average(this.world);
    }

    @MENetworkEventSubscribe
    public void onPowerStatusChange(final MENetworkPowerStatusChange event) {
        this.onPatternsChanged();
    }

    @MENetworkEventSubscribe
    public void onChannelsChanged(final MENetworkChannelsChanged event) {
        this.onPatternsChanged();
    }

    /**
     * A chamber is checked again once its controller is back, in case a wall went while it was unloaded - and a
     * chamber the old mod saved is assembled again here, having never said where its walls are. The check waits
     * for the end of the tick, when the chunks around have loaded too.
     */
    @Override
    public void onReady() {
        super.onReady();
        if (!this.assembled && !this.legacyAssembled) {
            return;
        }
        TickHandler.INSTANCE.addCallable(this.world, world -> {
            this.recheck();
            return null;
        });
    }

    private void recheck() {
        if (this.isInvalid() || (!this.assembled && !this.legacyAssembled)) {
            return;
        }
        if (this.min != null && this.max != null && !this.world.isAreaLoaded(this.min, this.max)) {
            return;
        }
        final AssemblerStructure.Outcome outcome = AssemblerStructure.check(this.world, this.pos);
        if (outcome.layout != null) {
            this.assemble(outcome.layout);
        } else {
            this.disassemble();
        }
    }

    @Override
    public boolean canBeRotated() {
        return false;
    }

    @Override
    public void readFromNBT(final NBTTagCompound data) {
        // The old mod's library kept the network node under a name of its own
        final boolean legacy = data.hasKey("MultiBlock");
        if (legacy && !data.hasKey("proxy")) {
            data.setTag("proxy", data.getCompoundTag("aeproxy"));
        }
        super.readFromNBT(data);

        if (legacy) {
            this.legacyAssembled = data.getCompoundTag("MultiBlock").getBoolean("Formed");
            this.readLegacyWork(data);
            return;
        }

        this.assembled = data.getBoolean("assembled");
        if (this.assembled) {
            this.min = BlockPos.fromLong(data.getLong("min"));
            this.max = BlockPos.fromLong(data.getLong("max"));
            this.parallel = data.getInteger("parallel");
            final NBTTagList modules = data.getTagList("patternModules", Constants.NBT.TAG_LONG);
            this.patternModules = new ArrayList<>(modules.tagCount());
            for (int i = 0; i < modules.tagCount(); i++) {
                this.patternModules.add(BlockPos.fromLong(((NBTTagLong) modules.get(i)).getLong()));
            }
        }
        this.legacyAssembled = data.getBoolean("legacyAssembled");
        this.work.readFromNBT(data.getTagList("work", Constants.NBT.TAG_COMPOUND));
    }

    /**
     * The old mod's work: each queued job becomes a batch of one, starting over, with what it was going to make;
     * the ingredients it kept beside it are already paid for by that and are not needed. Its output buffer is
     * work already done.
     */
    private void readLegacyWork(final NBTTagCompound data) {
        final NBTTagList queue = data.getCompoundTag("JobQueue").getTagList("Queue", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < queue.tagCount(); i++) {
            final NBTTagCompound job = queue.getCompoundTagAt(i);
            final List<GenericStack> outputs = new ArrayList<>();
            addLegacyStack(outputs, job.getCompoundTag("Result"));
            final NBTTagList remaining = job.getTagList("Remaining", Constants.NBT.TAG_COMPOUND);
            for (int j = 0; j < remaining.tagCount(); j++) {
                addLegacyStack(outputs, remaining.getCompoundTagAt(j));
            }
            if (!outputs.isEmpty()) {
                this.work.add(1, outputs);
            }
        }

        final List<GenericStack> done = new ArrayList<>();
        final NBTTagList buffer = data.getCompoundTag("OutputBuffer").getTagList("Items", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < buffer.tagCount(); i++) {
            addLegacyStack(done, buffer.getCompoundTagAt(i));
        }
        this.work.addFinished(done);
    }

    private static void addLegacyStack(final List<GenericStack> into, final NBTTagCompound tag) {
        if (tag.getKeySet().isEmpty() || tag.getBoolean("Empty")) {
            return;
        }
        final ItemStack stack = new ItemStack(tag);
        final AEItemKey key = AEItemKey.of(stack);
        if (key != null) {
            into.add(new GenericStack(key, stack.getCount()));
        }
    }

    @Override
    public NBTTagCompound writeToNBT(final NBTTagCompound data) {
        super.writeToNBT(data);
        if (this.assembled && this.min != null && this.max != null) {
            data.setBoolean("assembled", true);
            data.setLong("min", this.min.toLong());
            data.setLong("max", this.max.toLong());
            data.setInteger("parallel", this.parallel);
            final NBTTagList modules = new NBTTagList();
            for (final BlockPos at : this.patternModules) {
                modules.appendTag(new NBTTagLong(at.toLong()));
            }
            data.setTag("patternModules", modules);
        }
        if (this.legacyAssembled) {
            data.setBoolean("legacyAssembled", true);
        }
        if (!this.work.isEmpty()) {
            data.setTag("work", this.work.writeToNBT());
        }
        return data;
    }

    @Override
    protected boolean readFromStream(final ByteBuf data) throws IOException {
        final boolean changed = super.readFromStream(data);
        final boolean nowAssembled = data.readBoolean();
        if (nowAssembled == this.assembled) {
            return changed;
        }
        this.assembled = nowAssembled;
        return true;
    }

    @Override
    protected void writeToStream(final ByteBuf data) throws IOException {
        super.writeToStream(data);
        data.writeBoolean(this.assembled);
    }
}
