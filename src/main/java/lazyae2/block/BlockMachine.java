/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.block;

import java.util.Locale;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import lazyae2.LazyAE2;
import lazyae2.core.LazyAE2Config;
import lazyae2.core.LazyAE2Tab;
import lazyae2.tile.TileAggregator;
import lazyae2.tile.TileCentrifuge;
import lazyae2.tile.TileEnergizer;
import lazyae2.tile.TileEtcher;
import lazyae2.tile.TileProcessor;
import appeng.util.Platform;

/**
 * Every machine of this mod is one block with a subtype each, as the old mod had it: the subtype is the
 * metadata a saved world holds.
 */
public final class BlockMachine extends Block {

    public static final PropertyEnum<Type> TYPE = PropertyEnum.create("type", Type.class);
    public static final PropertyDirection FACING = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);
    public static final PropertyBool ACTIVE = PropertyBool.create("active");

    /**
     * Each machine keeps the metadata the old mod gave it, whatever order they are written in here: the
     * metadata is how a machine a world holds is found again.
     */
    public enum Type implements IStringSerializable {

        AGGREGATOR(0, TileAggregator::new),
        CENTRIFUGE(1, TileCentrifuge::new),
        ETCHER(2, TileEtcher::new),
        ENERGIZER(5, TileEnergizer::new);

        private static final Type[] VALUES = values();

        private final int meta;
        private final Supplier<TileProcessor> factory;

        Type(final int meta, final Supplier<TileProcessor> factory) {
            this.meta = meta;
            this.factory = factory;
        }

        public int getMeta() {
            return this.meta;
        }

        @Override
        public String getName() {
            return this.name().toLowerCase(Locale.ROOT);
        }

        public TileProcessor createTile() {
            return this.factory.get();
        }

        public boolean isEnabled() {
            return LazyAE2Config.instance().isEnabled(this.getName());
        }

        /**
         * A metadata no machine claims - one of this mod's own, not written yet - reads as the first.
         */
        public static Type of(final int meta) {
            for (final Type type : VALUES) {
                if (type.meta == meta) {
                    return type;
                }
            }
            return AGGREGATOR;
        }

        public static Type[] all() {
            return VALUES;
        }
    }

    public BlockMachine() {
        super(Material.IRON);
        this.setHardness(6F);
        this.setCreativeTab(LazyAE2Tab.INSTANCE);
        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(TYPE, Type.AGGREGATOR)
                .withProperty(FACING, EnumFacing.NORTH)
                .withProperty(ACTIVE, false));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, new IProperty[] { TYPE, FACING, ACTIVE });
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
        if (!(tile instanceof TileProcessor)) {
            return state;
        }
        final TileProcessor machine = (TileProcessor) tile;
        return state.withProperty(FACING, machine.getFront()).withProperty(ACTIVE, machine.isWorking());
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
        return state.getValue(TYPE).createTile();
    }

    @Override
    public void onBlockPlacedBy(final World world, final BlockPos pos, final IBlockState state,
            final EntityLivingBase placer, final ItemStack stack) {
        final TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileProcessor) {
            ((TileProcessor) tile).setFront(placer.getHorizontalFacing().getOpposite());
        }
    }

    @Override
    public boolean onBlockActivated(final World world, final BlockPos pos, final IBlockState state,
            final EntityPlayer player, final EnumHand hand, final EnumFacing facing, final float hitX, final float hitY,
            final float hitZ) {
        if (world.isRemote) {
            return true;
        }

        final ItemStack held = player.getHeldItem(hand);
        if (player.isSneaking() && Platform.isWrench(player, held, pos)) {
            this.dropBlockAsItem(world, pos, state, 0);
            world.setBlockToAir(pos);
            return true;
        }

        final TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileProcessor) {
            player.openGui(LazyAE2.instance, state.getValue(TYPE).getMeta(), world, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    @Override
    public boolean rotateBlock(final World world, final BlockPos pos, final EnumFacing axis) {
        final TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileProcessor)) {
            return false;
        }
        final TileProcessor machine = (TileProcessor) tile;
        if (axis == EnumFacing.UP) {
            machine.setFront(machine.getFront().rotateY());
        } else if (axis == EnumFacing.DOWN) {
            machine.setFront(machine.getFront().rotateYCCW());
        } else {
            machine.setFront(axis == machine.getFront() ? axis.getOpposite() : axis);
        }
        return true;
    }

    @Nullable
    @Override
    public EnumFacing[] getValidRotations(final World world, final BlockPos pos) {
        return EnumFacing.HORIZONTALS;
    }

    @Override
    public void breakBlock(final World world, final BlockPos pos, final IBlockState state) {
        final TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileProcessor) {
            final NonNullList<ItemStack> drops = NonNullList.create();
            ((TileProcessor) tile).getDrops(world, pos, drops);
            for (final ItemStack drop : drops) {
                Block.spawnAsEntity(world, pos, drop);
            }
        }
        super.breakBlock(world, pos, state);
    }

    @Override
    public void getSubBlocks(final CreativeTabs tab, final NonNullList<ItemStack> items) {
        for (final Type type : Type.all()) {
            if (type.isEnabled()) {
                items.add(new ItemStack(this, 1, type.getMeta()));
            }
        }
    }
}
