/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.item;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import lazyae2.Tags;
import lazyae2.block.BlockAssembler;
import lazyae2.tile.AssemblerStructure;
import appeng.core.MultiblockLimits;

/**
 * The chamber's blocks as items: one item with a subtype each, like the block it places.
 */
public final class ItemBlockAssembler extends ItemBlock {

    public ItemBlockAssembler(final BlockAssembler block) {
        super(block);
        this.setHasSubtypes(true);
    }

    @Override
    public int getMetadata(final int damage) {
        return damage;
    }

    @Override
    public String getTranslationKey(final ItemStack stack) {
        return "tile." + Tags.MOD_ID + ".big_assembler." + BlockAssembler.Type.of(stack.getMetadata()).getName();
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(final ItemStack stack, @Nullable final World world, final List<String> lines,
            final ITooltipFlag flag) {
        final BlockAssembler.Type type = BlockAssembler.Type.of(stack.getMetadata());
        if (type == BlockAssembler.Type.CONTROLLER) {
            lines.add(I18n.format("tooltip.threng.assembler.controller"));
            final MultiblockLimits.Limit limit = MultiblockLimits.get(AssemblerStructure.LIMIT);
            if (limit != null) {
                limit.addTooltip(lines);
            }
        } else if (type.getParallel() > 0) {
            lines.add(I18n.format("tooltip.threng.assembler.parallel", type.getParallel()));
        }
    }
}
