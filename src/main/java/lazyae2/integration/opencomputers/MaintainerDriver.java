/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.integration.opencomputers;

import javax.annotation.Nullable;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import li.cil.oc.api.Driver;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.internal.Database;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;

import lazyae2.block.BlockMachine;
import lazyae2.core.Registration;
import lazyae2.tile.TileLevelMaintainer;

/**
 * The ME Level Maintainer as an OpenComputers component, under the old mod's name and with its calls, and a few
 * more for what a row can hold and do now.
 */
public final class MaintainerDriver implements DriverBlock {

    private static final String NAME = "me_level_maintainer";

    /** Called only with OpenComputers installed, so its classes load with it and never without. */
    public static void register() {
        Driver.add(new MaintainerDriver());
        Driver.add(new Provider());
    }

    @Override
    public boolean worksWith(final World world, final BlockPos pos, final EnumFacing face) {
        return world.getTileEntity(pos) instanceof TileLevelMaintainer;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final BlockPos pos, final EnumFacing face) {
        final TileEntity tile = world.getTileEntity(pos);
        return tile instanceof TileLevelMaintainer ? new Component((TileLevelMaintainer) tile) : null;
    }

    public static final class Component extends AbstractManagedEnvironment implements NamedBlock {

        private final MaintainerCalls calls;

        public Component(final TileLevelMaintainer tile) {
            this.calls = new MaintainerCalls(tile);
            this.setNode(Network.newNode(this, Visibility.Network).withComponent(NAME).create());
        }

        @Override
        public String preferredName() {
            return NAME;
        }

        @Override
        public int priority() {
            return 5;
        }

        @Callback(doc = "function(index:number):boolean -- Checks whether a request is valid or not.")
        public Object[] isRequestValid(final Context ctx, final Arguments args) {
            return new Object[] { this.calls.isRequestValid(args.checkInteger(0)) };
        }

        @Callback(doc = "function([index:number]) -- Clears a request, or clears all requests if no index is specified.")
        public Object[] clearRequest(final Context ctx, final Arguments args) {
            if (args.count() > 0) {
                this.calls.clearRequest(args.checkInteger(0));
            } else {
                this.calls.clearAll();
            }
            return new Object[0];
        }

        @Callback(doc = "function(index:number):table -- Gets the item requested at the given index.")
        public Object[] getRequestItem(final Context ctx, final Arguments args) {
            final ItemStack stack = this.calls.getRequestItem(args.checkInteger(0));
            return new Object[] { stack.isEmpty() ? null : stack };
        }

        @Callback(doc = "function(index:number, dbAddress:string[, slot:number[, requestCount:number[, batchSize:number]]]) -- Sets a new request item from the database.")
        public Object[] setRequestItem(final Context ctx, final Arguments args) {
            final int row = args.checkInteger(0);
            final ItemStack stack = database(this.node(), args.checkString(1)).getStackInSlot(args.optInteger(2, 0));
            this.calls.setRequestItem(row, stack.copy(), optAmount(args, 3), optAmount(args, 4));
            return new Object[0];
        }

        @Callback(doc = "function(index:number, fluid:string[, requestCount:number[, batchSize:number]]) -- Sets a new request fluid by its name; amounts are in millibuckets.")
        public Object[] setRequestFluid(final Context ctx, final Arguments args) {
            this.calls.setRequestFluid(args.checkInteger(0), args.checkString(1), optAmount(args, 2),
                    optAmount(args, 3));
            return new Object[0];
        }

        @Callback(doc = "function(index:number, key:string[, requestCount:number[, batchSize:number]]) -- Sets a new request of anything, by the key getRequest gives.")
        public Object[] setRequestKey(final Context ctx, final Arguments args) {
            this.calls.setRequestKey(args.checkInteger(0), args.checkString(1), optAmount(args, 2),
                    optAmount(args, 3));
            return new Object[0];
        }

        @Callback(doc = "function(index:number):table -- Gets everything about a request: type, name, label, key, quantity, batchSize, enabled and state; nil for an empty one.")
        public Object[] getRequest(final Context ctx, final Arguments args) {
            return new Object[] { this.calls.getRequest(args.checkInteger(0)) };
        }

        @Callback(doc = "function(index:number):number -- Gets the quantity to maintain at the given index.")
        public Object[] getRequestQuantity(final Context ctx, final Arguments args) {
            return new Object[] { this.calls.getRequestQuantity(args.checkInteger(0)) };
        }

        @Callback(doc = "function(index:number, requestCount:number) -- Sets the quantity of a requested item to maintain.")
        public Object[] setRequestQuantity(final Context ctx, final Arguments args) {
            this.calls.setRequestQuantity(args.checkInteger(0), Math.round(args.checkDouble(1)));
            return new Object[0];
        }

        @Callback(doc = "function(index:number):number -- Gets the request batch size at the given index.")
        public Object[] getRequestBatchSize(final Context ctx, final Arguments args) {
            return new Object[] { this.calls.getRequestBatchSize(args.checkInteger(0)) };
        }

        @Callback(doc = "function(index:number, batchSize:number) -- Sets the batch size of a requested item.")
        public Object[] setRequestBatchSize(final Context ctx, final Arguments args) {
            this.calls.setRequestBatchSize(args.checkInteger(0), Math.round(args.checkDouble(1)));
            return new Object[0];
        }

        @Callback(doc = "function(index:number):boolean -- Checks whether a request is switched on.")
        public Object[] isRequestEnabled(final Context ctx, final Arguments args) {
            return new Object[] { this.calls.isRequestEnabled(args.checkInteger(0)) };
        }

        @Callback(doc = "function(index:number, enabled:boolean) -- Switches a request on or off; off, it keeps its settings and orders nothing.")
        public Object[] setRequestEnabled(final Context ctx, final Arguments args) {
            this.calls.setRequestEnabled(args.checkInteger(0), args.checkBoolean(1));
            return new Object[0];
        }
    }

    /** What an Adapter holding the machine as an item offers. */
    public static final class Provider implements EnvironmentProvider {

        @Nullable
        @Override
        public Class<?> getEnvironment(final ItemStack stack) {
            return Registration.machine != null && stack.getItem() == Item.getItemFromBlock(Registration.machine)
                    && BlockMachine.Type.of(stack.getMetadata()) == BlockMachine.Type.LEVEL_MAINTAINER
                    ? Component.class
                    : null;
        }
    }

    @Nullable
    private static Long optAmount(final Arguments args, final int index) {
        return args.count() > index ? Math.round(args.checkDouble(index)) : null;
    }

    private static Database database(final Node node, final String address) {
        final Node found = node.network().node(address);
        final Environment host = found == null ? null : found.host();
        if (found == null) {
            throw new IllegalArgumentException("no such component");
        }
        if (!(host instanceof Database)) {
            throw new IllegalArgumentException("not a database");
        }
        return (Database) host;
    }
}
