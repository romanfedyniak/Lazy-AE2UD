/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.container;

import net.minecraft.entity.player.InventoryPlayer;

import lazyae2.tile.TileProcessor;
import lazyae2.util.IoMode;
import lazyae2.util.RelativeSide;
import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.container.AEBaseContainer;
import appeng.container.guisync.GuiSync;
import appeng.container.slot.SlotRestrictedInput;
import appeng.util.Platform;

/**
 * What all four machines show: upgrade slots, how far along the work is, how much power is in the buffer and
 * which faces let items through.
 * <p>
 * Not {@code ContainerUpgradeable}, deliberately: that one offers the Network Tool's box, which is nine slots
 * of nothing to a machine that is on no network, and in a window this size the box would sit on the upgrade
 * column.
 */
public abstract class ContainerProcessor extends AEBaseContainer {

    public static final int HEIGHT = 166;
    /** Where the column of upgrade slots runs, just right of the window. */
    public static final int UPGRADE_LEFT = 187;
    public static final int UPGRADE_TOP = 8;

    private final TileProcessor machine;

    @GuiSync(20)
    public int work;
    @GuiSync(21)
    public int maxWork = 1;
    @GuiSync(22)
    public int energy;
    @GuiSync(23)
    public int maxEnergy = 1;
    @GuiSync(24)
    public YesNo autoExport = YesNo.NO;

    protected ContainerProcessor(final InventoryPlayer ip, final TileProcessor machine) {
        super(ip, machine, null);
        this.machine = machine;

        for (int slot = 0; slot < machine.getUpgradeInventory().getSlots(); slot++) {
            this.addSlotToContainer(new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.UPGRADES,
                    machine.getUpgradeInventory(), slot, UPGRADE_LEFT, UPGRADE_TOP + slot * 18, ip).setNotDraggable());
        }

        this.setupSlots(machine);
        this.bindPlayerInventory(ip, 0, HEIGHT - 82);
    }

    /**
     * The slots of this particular machine, added after the upgrades.
     */
    protected abstract void setupSlots(TileProcessor machine);

    public TileProcessor getMachine() {
        return this.machine;
    }

    public IoMode getFace(final RelativeSide side) {
        return this.machine.getSides().get(side);
    }

    public float getWorkFraction() {
        return this.maxWork <= 0 ? 0F : Math.min(1F, (float) this.work / this.maxWork);
    }

    public float getEnergyFraction() {
        return this.maxEnergy <= 0 ? 0F : Math.min(1F, (float) this.energy / this.maxEnergy);
    }

    public int getEnergyStored() {
        return this.energy;
    }

    public int getMaxEnergyStored() {
        return this.maxEnergy;
    }

    @Override
    public void detectAndSendChanges() {
        if (Platform.isServer()) {
            this.work = this.machine.getWork();
            this.maxWork = this.machine.getMaxWork();
            this.energy = this.machine.getEnergy().getEnergyStored();
            this.maxEnergy = this.machine.getEnergy().getMaxEnergyStored();
            this.autoExport = (YesNo) this.machine.getConfigManager().getSetting(Settings.AUTO_EXPORT);
        }
        super.detectAndSendChanges();
    }
}
