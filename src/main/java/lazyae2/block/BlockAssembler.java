/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.block;

import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import lazyae2.core.LazyAE2Config;
import lazyae2.core.LazyAE2Tab;
import lazyae2.core.Registration;
import lazyae2.tile.TileAssemblerController;
import lazyae2.tile.TileAssemblerIoPort;
import lazyae2.tile.TileAssemblerPart;
import lazyae2.tile.TileAssemblerPatterns;
import appeng.util.Platform;

/**
 * Every block of the Mass Assembly Chamber, as one block with a subtype each - the old mod's metadata, so a
 * chamber a world holds keeps its blocks.
 */
public final class BlockAssembler extends Block {

    public static final PropertyEnum<Type> TYPE = PropertyEnum.create("type", Type.class);
    public static final PropertyBool ACTIVE = PropertyBool.create("active");

    public enum Type implements IStringSerializable {

        FRAME(0, 0),
        VENT(1, 0),
        CONTROLLER(2, 0),
        IO_PORT(5, 0),
        MODULE_PATTERN(3, 0),
        MODULE_CPU(4, 1),
        MODULE_CPU_4X(6, 4),
        MODULE_CPU_16X(7, 16),
        MODULE_CPU_64X(8, 64),
        MODULE_CPU_256X(9, 256);

        private static final Type[] VALUES = values();

        private final int meta;
        private final int parallel;

        Type(final int meta, final int parallel) {
            this.meta = meta;
            this.parallel = parallel;
        }

        public int getMeta() {
            return this.meta;
        }

        /** How many crafts at once this block adds to a chamber: nothing, unless it is a co-processor. */
        public int getParallel() {
            return this.parallel;
        }

        /** What goes on the twelve edges. */
        public boolean isEdge() {
            return this == FRAME;
        }

        /** What goes on the six faces between the edges. */
        public boolean isWall() {
            return this == VENT || this == CONTROLLER || this == IO_PORT;
        }

        /** What goes inside, where air does too. */
        public boolean isModule() {
            return this == MODULE_PATTERN || this.parallel > 0;
        }

        public ItemStack newStack(final int count) {
            return Registration.assembler == null ? ItemStack.EMPTY
                    : new ItemStack(Registration.assembler, count, this.meta);
        }

        @Override
        public String getName() {
            return this.name().toLowerCase(Locale.ROOT);
        }

        public static Type of(final int meta) {
            for (final Type type : VALUES) {
                if (type.meta == meta) {
                    return type;
                }
            }
            return FRAME;
        }

        public static Type[] all() {
            return VALUES;
        }
    }

    public BlockAssembler() {
        super(Material.IRON);
        this.setHardness(6F);
        this.setCreativeTab(LazyAE2Tab.INSTANCE);
        this.setDefaultState(this.blockState.getBaseState().withProperty(TYPE, Type.FRAME).withProperty(ACTIVE, false));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, new IProperty[] { TYPE, ACTIVE });
    }

    @Override
    public IBlockState getStateFromMeta(final int meta) {
        return this.getDefaultState().withProperty(TYPE, Type.of(meta));
    }

    @Override
    public int getMetaFromState(final IBlockState state) {
        return state.getValue(TYPE).getMeta();
    }

    @SuppressWarnings("deprecation")
    @Override
    public IBlockState getActualState(final IBlockState state, final IBlockAccess world, final BlockPos pos) {
        final TileEntity tile = world.getTileEntity(pos);
        return state.withProperty(ACTIVE, tile instanceof IAssemblerBlock && ((IAssemblerBlock) tile).isAssembled());
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getLightValue(final IBlockState state) {
        return state.getValue(TYPE).isModule() ? 11 : 0;
    }

    @Override
    public int damageDropped(final IBlockState state) {
        return this.getMetaFromState(state);
    }

    @Override
    public boolean hasTileEntity(final IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(final World world, final IBlockState state) {
        switch (state.getValue(TYPE)) {
            case CONTROLLER:
                return new TileAssemblerController();
            case MODULE_PATTERN:
                return new TileAssemblerPatterns();
            case IO_PORT:
                return new TileAssemblerIoPort();
            default:
                return new TileAssemblerPart();
        }
    }

    /**
     * A click with a block in hand places it, as a click on any block does, so a chamber can be built against
     * its own walls. Every other click is the chamber's.
     */
    @Override
    public boolean onBlockActivated(final World world, final BlockPos pos, final IBlockState state,
            final EntityPlayer player, final EnumHand hand, final EnumFacing facing, final float hitX, final float hitY,
            final float hitZ) {
        final ItemStack held = player.getHeldItem(hand);
        if (player.isSneaking() && Platform.isWrench(player, held, pos)) {
            if (!world.isRemote) {
                this.dropBlockAsItem(world, pos, state, 0);
                world.setBlockToAir(pos);
            }
            return true;
        }
        if (held.getItem() instanceof ItemBlock) {
            return false;
        }
        if (world.isRemote) {
            return true;
        }

        final TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileAssemblerController) {
            ((TileAssemblerController) tile).toggleAssembly(player);
        } else if (tile instanceof IAssemblerBlock && !((IAssemblerBlock) tile).isAssembled()) {
            player.sendMessage(message("chat.threng.assembler.clickController", TextFormatting.GRAY));
        }
        return true;
    }

    @Override
    public void breakBlock(final World world, final BlockPos pos, final IBlockState state) {
        final TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileAssemblerController) {
            final NonNullList<ItemStack> drops = NonNullList.create();
            ((TileAssemblerController) tile).onBroken(drops);
            for (final ItemStack drop : drops) {
                Block.spawnAsEntity(world, pos, drop);
            }
        } else if (tile instanceof TileAssemblerPart) {
            final TileAssemblerController controller = ((TileAssemblerPart) tile).getController();
            if (controller != null) {
                controller.disassemble();
            }
            if (tile instanceof TileAssemblerPatterns) {
                final NonNullList<ItemStack> drops = NonNullList.create();
                ((TileAssemblerPatterns) tile).getDrops(world, pos, drops);
                for (final ItemStack drop : drops) {
                    Block.spawnAsEntity(world, pos, drop);
                }
            }
        }
        super.breakBlock(world, pos, state);
    }

    @Override
    public void getSubBlocks(final CreativeTabs tab, final NonNullList<ItemStack> items) {
        if (!LazyAE2Config.instance().isMassAssemblerEnabled()) {
            return;
        }
        for (final Type type : Type.all()) {
            items.add(new ItemStack(this, 1, type.getMeta()));
        }
    }

    public static TextComponentTranslation message(final String key, final TextFormatting colour,
            final Object... args) {
        final TextComponentTranslation text = new TextComponentTranslation(key, args);
        text.getStyle().setColor(colour);
        return text;
    }

    /** Anything that is part of a chamber and can say whether that chamber is assembled. */
    public interface IAssemblerBlock {

        boolean isAssembled();
    }
}
