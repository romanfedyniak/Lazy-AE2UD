/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import lazyae2.core.ModGuiBridges;
import appeng.api.implementations.IUpgradeableCellContainer;
import appeng.container.implementations.WirelessTerminalSupport;
import appeng.container.interfaces.IInventorySlotAware;
import appeng.container.interfaces.IWirelessTerminalContainer;
import appeng.container.slot.SlotRestrictedInput;
import appeng.core.sync.GuiBridge;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.util.Platform;

/**
 * The same terminal carried in a pocket: the window is unchanged, and what is added to it is the terminal
 * itself - its charge, its range and the cards in it.
 */
public final class ContainerWirelessLevelMaintainerTerminal extends ContainerLevelMaintainerTerminal
        implements IInventorySlotAware, IUpgradeableCellContainer, IWirelessTerminalContainer {

    private final WirelessTerminalSupport support;
    private final List<Slot> upgradeSlots = new ArrayList<>();

    public ContainerWirelessLevelMaintainerTerminal(final InventoryPlayer ip,
            final WirelessTerminalGuiObject guiObject) {
        super(ip, guiObject, false);

        this.support = new WirelessTerminalSupport(this, guiObject, UPGRADE_SLOTS);
        this.bindPlayerInventory(ip, 14, HEIGHT - /* height of player inventory */82);
        this.setupUpgrades();
    }

    @Override
    public GuiBridge getOriginGui() {
        return ModGuiBridges.wirelessLevelMaintainerTerminal();
    }

    @Override
    public void detectAndSendChanges() {
        if (Platform.isServer()) {
            this.support.tick();
            super.detectAndSendChanges();
        }
    }

    @Override
    public ItemStack slotClick(final int slotId, final int dragType, final ClickType clickType,
            @Nonnull final EntityPlayer player) {
        if (slotId >= 0 && slotId < this.inventorySlots.size()
                && this.upgradeSlots.contains(this.inventorySlots.get(slotId))
                && WirelessTerminalSupport.toggleMagnetCard(this.inventorySlots.get(slotId), dragType, clickType)) {
            return ItemStack.EMPTY;
        }

        return super.slotClick(slotId, dragType, clickType, player);
    }

    @Override
    public ItemStack getTerminal() {
        return this.support.getTerminal();
    }

    @Override
    public int getInventorySlot() {
        return this.support.getInventorySlot();
    }

    @Override
    public boolean isBaubleSlot() {
        return this.support.isBaubleSlot();
    }

    @Override
    public int availableUpgrades() {
        return UPGRADE_SLOTS;
    }

    @Override
    public void setupUpgrades() {
        for (int upgradeSlot = 0; upgradeSlot < this.availableUpgrades(); upgradeSlot++) {
            final SlotRestrictedInput slot = new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.UPGRADES,
                    this.support.getUpgrades(), upgradeSlot, 201, 169 + upgradeSlot * 18, this.getInventoryPlayer());
            slot.setNotDraggable();
            this.upgradeSlots.add(this.addSlotToContainer(slot));
        }
    }
}
