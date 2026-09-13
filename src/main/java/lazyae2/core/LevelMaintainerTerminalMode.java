/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.core;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.IGuiHandler;

import lazyae2.Tags;
import appeng.api.features.IWirelessTerminalMode;

/**
 * The terminal as one of the faces AE2's wireless terminal can be switched to. Crafting the part into a
 * wireless terminal is what unlocks it; AE2 builds that recipe, and a key to open it, out of this.
 */
public final class LevelMaintainerTerminalMode implements IWirelessTerminalMode {

    public static final ResourceLocation ID = new ResourceLocation(Tags.MOD_ID, "level_maintainer_terminal");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    /**
     * Asked for each time rather than kept: the part is registered after this mode is, and a terminal the
     * config has taken away has no item to show at all.
     */
    @Override
    public ItemStack getIcon() {
        return Registration.terminal == null ? ItemStack.EMPTY : new ItemStack(Registration.terminal);
    }

    @Override
    public String getUnlocalizedName() {
        return "gui.threng.wirelessMode.levelMaintainer";
    }

    @Override
    public IGuiHandler getGuiHandler() {
        return ModGuiBridges.wirelessLevelMaintainerTerminal();
    }

    @Override
    public ItemStack getUnlockIngredient() {
        return this.getIcon();
    }

    @Override
    public String toString() {
        return ID.toString();
    }
}
