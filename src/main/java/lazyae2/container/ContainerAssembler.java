/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.container;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;

import lazyae2.core.LazyAE2Config;
import lazyae2.network.ModNetwork;
import lazyae2.network.PacketAssemblerPatterns;
import lazyae2.tile.TileAssemblerController;
import lazyae2.tile.TileAssemblerPatterns;
import appeng.client.me.SlotDisconnected;
import appeng.container.AEBaseContainer;
import appeng.container.guisync.GuiSync;
import appeng.container.slot.AppEngSlot;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketInventoryAction;
import appeng.helpers.InventoryAction;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;

import static appeng.helpers.ItemStackHelper.stackWriteToNBT;

/**
 * The chamber's window: every pattern of every module, and how busy the chamber is.
 * <p>
 * The patterns are no slots of this container - a chamber can hold thousands - so the window keeps a copy the
 * server sends it, one module at a time and only when a module changed, and a click names the module and the
 * slot, as the Pattern Access Terminal does.
 */
public final class ContainerAssembler extends AEBaseContainer {

    /** How many modules go into one packet, which keeps any one packet small however large the chamber. */
    private static final int MODULES_PER_PACKET = 8;

    private final TileAssemblerController controller;
    /** Each module by its position, with what the window was last told it holds. */
    private final Map<Long, Tracker> trackers = new LinkedHashMap<>();

    @GuiSync(40)
    public int busy;
    @GuiSync(41)
    public int parallel;
    @GuiSync(42)
    public int patterns;
    @GuiSync(43)
    public int patternSlots;
    /** AE per tick, times a hundred. */
    @GuiSync(44)
    public long power;
    @GuiSync(45)
    public int ticksPerJob = 10;

    public ContainerAssembler(final InventoryPlayer ip, final TileAssemblerController controller) {
        super(ip, controller, null);
        this.controller = controller;
        this.bindPlayerInventory(ip, 0, 0);
    }

    public TileAssemblerController getController() {
        return this.controller;
    }

    @Override
    public boolean canInteractWith(final EntityPlayer player) {
        return super.canInteractWith(player) && this.controller.isAssembled();
    }

    @Override
    public void detectAndSendChanges() {
        if (Platform.isServer()) {
            this.updateNumbers();
            this.sendPatterns();
        }
        super.detectAndSendChanges();
    }

    private void updateNumbers() {
        this.busy = this.controller.getBusy();
        this.parallel = this.controller.getParallel();
        this.power = (long) ((this.controller.getProxy().getIdlePowerUsage() + this.controller.getActivePowerUsage())
                * 100);
        this.ticksPerJob = LazyAE2Config.instance().getMassAssemblerTicksPerJob();

        int filled = 0;
        int slots = 0;
        for (final TileAssemblerPatterns module : this.controller.getPatternModules()) {
            final IItemHandler inventory = module.getPatterns();
            slots += inventory.getSlots();
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                if (!inventory.getStackInSlot(slot).isEmpty()) {
                    filled++;
                }
            }
        }
        this.patterns = filled;
        this.patternSlots = slots;
    }

    private void sendPatterns() {
        final List<TileAssemblerPatterns> modules = this.controller.getPatternModules();
        final List<NBTTagCompound> changed = new ArrayList<>();
        boolean clear = modules.size() != this.trackers.size();
        if (!clear) {
            for (final TileAssemblerPatterns module : modules) {
                if (!this.trackers.containsKey(module.getPos().toLong())) {
                    clear = true;
                    break;
                }
            }
        }

        if (clear) {
            this.trackers.clear();
        }

        int index = 0;
        for (final TileAssemblerPatterns module : modules) {
            final long id = module.getPos().toLong();
            Tracker tracker = this.trackers.get(id);
            if (tracker == null) {
                tracker = new Tracker(module.getPatterns());
                this.trackers.put(id, tracker);
            }

            final NBTTagCompound tag = new NBTTagCompound();
            for (int slot = 0; slot < tracker.server.getSlots(); slot++) {
                final ItemStack now = tracker.server.getStackInSlot(slot);
                if (clear || !ItemStack.areItemStacksEqual(now, tracker.client.getStackInSlot(slot))) {
                    tracker.client.setStackInSlot(slot, now.isEmpty() ? ItemStack.EMPTY : now.copy());
                    final NBTTagCompound item = new NBTTagCompound();
                    if (!now.isEmpty()) {
                        stackWriteToNBT(now, item);
                    }
                    tag.setTag(Integer.toString(slot), item);
                }
            }
            if (!tag.isEmpty()) {
                tag.setLong("id", id);
                tag.setInteger("index", index);
                changed.add(tag);
            }
            index++;
        }

        if (!clear && changed.isEmpty()) {
            return;
        }
        final EntityPlayerMP player = (EntityPlayerMP) this.getPlayerInv().player;
        for (int from = 0; from < changed.size() || (clear && from == 0); from += MODULES_PER_PACKET) {
            final List<NBTTagCompound> part =
                    new ArrayList<>(changed.subList(from, Math.min(changed.size(), from + MODULES_PER_PACKET)));
            ModNetwork.CHANNEL.sendTo(new PacketAssemblerPatterns(clear && from == 0, part), player);
        }
    }

    /**
     * A click on a pattern slot. Every insertion goes through the module's own inventory, so a processing
     * pattern or a second copy of one already in the chamber is refused here exactly as it is by a pipe.
     */
    @Override
    public void doAction(final EntityPlayerMP player, final InventoryAction action, final int slot, final long id) {
        final Tracker tracker = this.trackers.get(id);
        if (tracker == null) {
            super.doAction(player, action, slot, id);
            return;
        }
        final IItemHandlerModifiable inventory = tracker.server;

        if (action == InventoryAction.PLACE_SINGLE) {
            // Shift-click in the player's inventory: slot is theirs, and the pattern goes wherever it fits
            if (slot < 0 || slot >= this.inventorySlots.size()
                    || !(this.inventorySlots.get(slot) instanceof AppEngSlot)) {
                return;
            }
            final AppEngSlot playerSlot = (AppEngSlot) this.inventorySlots.get(slot);
            if (playerSlot.isPlayerSide() && playerSlot.getHasStack()) {
                playerSlot.putStack(ItemHandlerHelper.insertItem(inventory, playerSlot.getStack(), false));
                this.detectAndSendChanges();
            }
            return;
        }
        if (slot < 0 || slot >= inventory.getSlots()) {
            return;
        }

        final ItemStack inSlot = inventory.getStackInSlot(slot);
        final ItemStack inHand = player.inventory.getItemStack();

        switch (action) {
            case PICKUP_OR_SET_DOWN:
            case SPLIT_OR_PLACE_SINGLE:
                if (inHand.isEmpty()) {
                    player.inventory.setItemStack(inventory.extractItem(slot, inSlot.getCount(), false));
                } else if (inSlot.isEmpty()) {
                    final ItemStack one = ItemHandlerHelper.copyStackWithSize(inHand, 1);
                    if (inventory.insertItem(slot, one, false).isEmpty()) {
                        inHand.shrink(1);
                        player.inventory.setItemStack(inHand.isEmpty() ? ItemStack.EMPTY : inHand);
                    }
                } else if (inHand.getCount() == 1) {
                    // A swap: the pattern in hand has to pass the module's rules with the old one gone
                    final ItemStack taken = inventory.extractItem(slot, inSlot.getCount(), false);
                    if (inventory.insertItem(slot, inHand.copy(), false).isEmpty()) {
                        player.inventory.setItemStack(taken);
                    } else {
                        inventory.setStackInSlot(slot, taken);
                    }
                }
                break;
            case SHIFT_CLICK:
                inventory.setStackInSlot(slot, InventoryAdaptor.getAdaptor(player).addItems(inSlot));
                break;
            case MOVE_REGION:
                final InventoryAdaptor playerInventory = InventoryAdaptor.getAdaptor(player);
                for (int x = 0; x < inventory.getSlots(); x++) {
                    inventory.setStackInSlot(x, playerInventory.addItems(inventory.getStackInSlot(x)));
                }
                break;
            case CREATIVE_DUPLICATE:
                if (player.capabilities.isCreativeMode && inHand.isEmpty() && !inSlot.isEmpty()) {
                    player.inventory.setItemStack(inSlot.copy());
                }
                break;
            default:
                break;
        }
        this.updateHeld(player);
    }

    /** Shift-click in the player's inventory: the window asks the server to put it in the first free slot. */
    @Override
    public ItemStack transferStackInSlot(final EntityPlayer player, final int index) {
        if (Platform.isClient() && this.inventorySlots.get(index) instanceof AppEngSlot) {
            final AppEngSlot playerSlot = (AppEngSlot) this.inventorySlots.get(index);
            if (playerSlot.isPlayerSide()) {
                for (final Object slot : this.inventorySlots) {
                    if (slot instanceof SlotDisconnected && !((SlotDisconnected) slot).getHasStack()) {
                        NetworkHandler.instance().sendToServer(new PacketInventoryAction(InventoryAction.PLACE_SINGLE,
                                playerSlot.slotNumber, ((SlotDisconnected) slot).getSlot().getId()));
                        return ItemStack.EMPTY;
                    }
                }
            }
        }
        return super.transferStackInSlot(player, index);
    }

    private static final class Tracker {

        final IItemHandlerModifiable server;
        final AppEngInternalInventory client;

        Tracker(final IItemHandlerModifiable server) {
            this.server = server;
            this.client = new AppEngInternalInventory(null, server.getSlots());
        }
    }
}
