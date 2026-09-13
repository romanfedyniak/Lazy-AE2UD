/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.part;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;

import lazyae2.Tags;
import lazyae2.core.ModGuiBridges;
import appeng.api.parts.IPartModel;
import appeng.core.sync.GuiBridge;
import appeng.helpers.ISubMenuHost;
import appeng.parts.PartModel;
import appeng.parts.reporting.AbstractPartDisplay;
import appeng.util.Platform;

/**
 * The ME Level Maintainer Terminal: every maintainer on the network in one window, with the rows of each of
 * them editable from here.
 */
public final class PartLevelMaintainerTerminal extends AbstractPartDisplay implements ISubMenuHost {

    public static final ResourceLocation MODEL_OFF =
            new ResourceLocation(Tags.MOD_ID, "part/level_maintainer_terminal_off");
    public static final ResourceLocation MODEL_ON =
            new ResourceLocation(Tags.MOD_ID, "part/level_maintainer_terminal_on");

    public static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE, MODEL_OFF, MODEL_STATUS_OFF);
    public static final IPartModel MODELS_ON = new PartModel(MODEL_BASE, MODEL_ON, MODEL_STATUS_ON);
    public static final IPartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_BASE, MODEL_ON, MODEL_STATUS_HAS_CHANNEL);

    /** What the screen last had in each of its two search fields, kept while the chunk is loaded. */
    private String searchRows = "";
    private String searchNames = "";

    public PartLevelMaintainerTerminal(final ItemStack is) {
        super(is);
    }

    @Override
    public GuiBridge getGuiBridge() {
        return ModGuiBridges.levelMaintainerTerminal();
    }

    @Override
    public ItemStack getItemStackRepresentation() {
        return this.getItemStack();
    }

    @Override
    public boolean onPartActivate(final EntityPlayer player, final EnumHand hand, final Vec3d pos) {
        if (!super.onPartActivate(player, hand, pos)) {
            if (Platform.isServer()) {
                Platform.openGUI(player, this.getHost().getTile(), this.getSide(), this.getGuiBridge());
            }
        }
        return true;
    }

    @Override
    public IPartModel getStaticModels() {
        return this.selectModel(MODELS_OFF, MODELS_ON, MODELS_HAS_CHANNEL);
    }

    public String getSearchRows() {
        return this.searchRows;
    }

    public String getSearchNames() {
        return this.searchNames;
    }

    public void saveSearchStrings(final String rows, final String names) {
        this.searchRows = rows;
        this.searchNames = names;
    }
}
