/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.tile;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import lazyae2.Tags;
import lazyae2.block.BlockAssembler;
import lazyae2.core.LazyAE2Config;
import appeng.core.MultiblockLimits;

/**
 * Whether the blocks around a controller make a chamber: a hollow box with frames on its twelve edges, vents,
 * IO ports and the one controller on its faces, and modules or air inside, at least one of them a pattern
 * module.
 */
public final class AssemblerStructure {

    public static final ResourceLocation LIMIT = new ResourceLocation(Tags.MOD_ID, "mass_assembler");

    private AssemblerStructure() {
    }

    public static void registerLimit() {
        final LazyAE2Config config = LazyAE2Config.instance();
        MultiblockLimits.register(LIMIT, config::getMassAssemblerMaxSizeX, config::getMassAssemblerMaxSizeY,
                config::getMassAssemblerMaxSizeZ, config::isMassAssemblerSingleChunk);
    }

    /** A chamber that checked out. */
    public static final class Layout {

        public final BlockPos min;
        public final BlockPos max;
        /** How many crafts it runs at once: one of its own, plus what its co-processor modules add. */
        public final int parallel;
        public final List<BlockPos> patternModules;

        Layout(final BlockPos min, final BlockPos max, final int parallel, final List<BlockPos> patternModules) {
            this.min = min;
            this.max = max;
            this.parallel = parallel;
            this.patternModules = Collections.unmodifiableList(patternModules);
        }
    }

    /** Either a layout or the reason there is none. */
    public static final class Outcome {

        @Nullable
        public final Layout layout;
        @Nullable
        public final ITextComponent refusal;

        private Outcome(@Nullable final Layout layout, @Nullable final ITextComponent refusal) {
            this.layout = layout;
            this.refusal = refusal;
        }
    }

    public static Outcome check(final World world, final BlockPos controller) {
        final MultiblockLimits.Limit limit = MultiblockLimits.get(LIMIT);
        final int limitX = limit.getX();
        final int limitY = limit.getY();
        final int limitZ = limit.getZ();

        // Every chamber block connected to the controller. A box a player built touching another one is one
        // shape here, and fails as one.
        int minX = controller.getX(), minY = controller.getY(), minZ = controller.getZ();
        int maxX = minX, maxY = minY, maxZ = minZ;
        final Set<BlockPos> seen = new HashSet<>();
        final Deque<BlockPos> queue = new ArrayDeque<>();
        seen.add(controller);
        queue.add(controller);

        while (!queue.isEmpty()) {
            final BlockPos at = queue.poll();
            for (final EnumFacing direction : EnumFacing.VALUES) {
                final BlockPos next = at.offset(direction);
                if (seen.contains(next) || !world.isBlockLoaded(next) || !isChamberBlock(world.getBlockState(next))) {
                    continue;
                }
                seen.add(next);
                minX = Math.min(minX, next.getX());
                minY = Math.min(minY, next.getY());
                minZ = Math.min(minZ, next.getZ());
                maxX = Math.max(maxX, next.getX());
                maxY = Math.max(maxY, next.getY());
                maxZ = Math.max(maxZ, next.getZ());
                if (maxX - minX >= limitX || maxY - minY >= limitY || maxZ - minZ >= limitZ) {
                    return refuse("tooLarge", limitX, limitY, limitZ);
                }
                queue.add(next);
            }
        }

        if (maxX - minX < 2 || maxY - minY < 2 || maxZ - minZ < 2) {
            return refuse("tooSmall", maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1);
        }
        if (limit.requiresSingleChunk() && (minX >> 4 != maxX >> 4 || minZ >> 4 != maxZ >> 4)) {
            return refuse("acrossChunks");
        }

        final BlockPos min = new BlockPos(minX, minY, minZ);
        final BlockPos max = new BlockPos(maxX, maxY, maxZ);
        if (boundaries(controller, min, max) != 1) {
            return refuse("controllerOnEdge");
        }

        int parallel = 1;
        final List<BlockPos> patternModules = new ArrayList<>();

        for (final BlockPos.MutableBlockPos at : BlockPos.getAllInBoxMutable(min, max)) {
            if (!world.isBlockLoaded(at)) {
                return refuse("unloaded", at.getX(), at.getY(), at.getZ());
            }
            final IBlockState state = world.getBlockState(at);
            final BlockAssembler.Type type = isChamberBlock(state) ? state.getValue(BlockAssembler.TYPE) : null;
            final int boundaries = boundaries(at, min, max);

            if (boundaries >= 2) {
                if (type == null || !type.isEdge()) {
                    return refuse("needsFrame", at.getX(), at.getY(), at.getZ());
                }
            } else if (boundaries == 1) {
                if (type == null || !type.isWall()) {
                    return refuse("needsWall", at.getX(), at.getY(), at.getZ());
                }
                if (type == BlockAssembler.Type.CONTROLLER && !at.equals(controller)) {
                    return refuse("secondController", at.getX(), at.getY(), at.getZ());
                }
            } else if (type == null ? !world.isAirBlock(at) : !type.isModule()) {
                return refuse("needsModule", at.getX(), at.getY(), at.getZ());
            }

            if (type == null) {
                continue;
            }
            if (belongsElsewhere(world, at, controller)) {
                return refuse("otherChamber", at.getX(), at.getY(), at.getZ());
            }
            parallel += type.getParallel();
            if (type == BlockAssembler.Type.MODULE_PATTERN) {
                patternModules.add(at.toImmutable());
            }
        }

        if (patternModules.isEmpty()) {
            return refuse("noPatterns");
        }
        return new Outcome(new Layout(min, max, parallel, patternModules), null);
    }

    private static boolean isChamberBlock(final IBlockState state) {
        return state.getBlock() instanceof BlockAssembler;
    }

    /** On how many of the box's sides a position lies: none inside, one on a face, two or three on an edge. */
    private static int boundaries(final BlockPos at, final BlockPos min, final BlockPos max) {
        int count = 0;
        if (at.getX() == min.getX() || at.getX() == max.getX()) {
            count++;
        }
        if (at.getY() == min.getY() || at.getY() == max.getY()) {
            count++;
        }
        if (at.getZ() == min.getZ() || at.getZ() == max.getZ()) {
            count++;
        }
        return count;
    }

    /** Whether a block is already part of a chamber some other controller keeps assembled. */
    private static boolean belongsElsewhere(final World world, final BlockPos at, final BlockPos controller) {
        final TileEntity tile = world.getTileEntity(at);
        if (!(tile instanceof TileAssemblerPart)) {
            return false;
        }
        final BlockPos owner = ((TileAssemblerPart) tile).getControllerPos();
        if (owner == null || owner.equals(controller)) {
            return false;
        }
        final TileAssemblerController other = ((TileAssemblerPart) tile).getController();
        return other != null && other.isAssembled();
    }

    private static Outcome refuse(final String reason, final Object... args) {
        return new Outcome(null, BlockAssembler.message("chat.threng.assembler.refused." + reason, TextFormatting.RED, args));
    }
}
