/*
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.integration.opencomputers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.FMLInjectionData;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.registries.RegistryBuilder;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.AEKeyTypes;
import appeng.api.stacks.GenericStack;
import appeng.core.api.AEFluidKeyType;
import appeng.core.api.AEItemKeyType;

import lazyae2.core.LazyAE2Config;
import lazyae2.tile.TileLevelMaintainer;

/**
 * What a computer's calls do to a real maintainer, without a computer.
 */
final class MaintainerCallsTest {

    private TileLevelMaintainer tile;
    private MaintainerCalls calls;

    @BeforeAll
    static void setUp() throws Exception {
        Bootstrap.register();
        final File config = Files.createTempDirectory("lazyae2-config").toFile();
        config.deleteOnExit();
        final Field home = FMLInjectionData.class.getDeclaredField("minecraftHome");
        home.setAccessible(true);
        home.set(null, config);
        LazyAE2Config.init(config);
        fakeServerSide();
        if (GameRegistry.findRegistry(AEKeyType.class) == null) {
            new RegistryBuilder<AEKeyType>().setName(AEKeyTypes.REGISTRY_NAME).setType(AEKeyType.class)
                    .setIDRange(0, 127).create();
            AEKeyTypes.register(new AEItemKeyType());
        }
        if (AEKeyTypes.get(AEKeyTypes.FLUIDS_ID) == null) {
            AEKeyTypes.register(new AEFluidKeyType());
        }
        // The placeholder item that carries a fluid in a slot is registered by AE2 in a game, not here
        GenericStack.setWrapper(new GenericStack.Wrapper() {

            @Override
            public ItemStack wrap(final AEKey what, final long amount) {
                final ItemStack stack = new ItemStack(Items.PAPER);
                final NBTTagCompound tag = new NBTTagCompound();
                what.toTagGeneric(tag);
                stack.setTagInfo("wrapped", tag);
                return stack;
            }

            @Override
            public boolean isWrapped(final ItemStack stack) {
                return stack.getItem() == Items.PAPER && stack.getSubCompound("wrapped") != null;
            }

            @Override
            public GenericStack unwrap(final ItemStack stack) {
                return this.isWrapped(stack)
                        ? new GenericStack(AEKey.fromTagGeneric(stack.getSubCompound("wrapped")), 1)
                        : null;
            }
        });
    }

    private static void fakeServerSide() throws Exception {
        final Class<?> handlerType = Class.forName("net.minecraftforge.fml.common.IFMLSidedHandler");
        final Object delegate = Proxy.newProxyInstance(handlerType.getClassLoader(), new Class<?>[] { handlerType },
                (proxy, method, args) -> "getSide".equals(method.getName()) ? Side.SERVER : null);
        final Field field = FMLCommonHandler.class.getDeclaredField("sidedDelegate");
        field.setAccessible(true);
        field.set(FMLCommonHandler.instance(), delegate);
    }

    @BeforeEach
    void newMachine() {
        this.tile = new TileLevelMaintainer();
        this.calls = new MaintainerCalls(this.tile);
    }

    @Test
    void anItemSetTheOldWayReadsBackTheOldWay() {
        this.calls.setRequestItem(2, new ItemStack(Items.DIAMOND, 7), 64L, 16L);

        assertTrue(this.calls.isRequestValid(2));
        assertTrue(ItemStack.areItemsEqual(new ItemStack(Items.DIAMOND), this.calls.getRequestItem(2)));
        assertEquals(1, this.calls.getRequestItem(2).getCount());
        assertEquals(64, this.calls.getRequestQuantity(2));
        assertEquals(16, this.calls.getRequestBatchSize(2));
        assertFalse(this.calls.isRequestValid(1));
    }

    @Test
    void anItemWithoutAmountsKeepsOne() {
        this.calls.setRequestItem(0, new ItemStack(Items.DIAMOND), null, null);

        assertEquals(1, this.calls.getRequestQuantity(0));
        assertEquals(1, this.calls.getRequestBatchSize(0));
    }

    @Test
    void anEmptyDatabaseSlotClearsTheRow() {
        this.calls.setRequestItem(0, new ItemStack(Items.DIAMOND), 10L, null);
        this.calls.setRequestItem(0, ItemStack.EMPTY, 10L, null);

        assertFalse(this.calls.isRequestValid(0));
        assertEquals(0, this.calls.getRequestQuantity(0));
    }

    @Test
    void amountsOnAnEmptyRowAreIgnored() {
        this.calls.setRequestQuantity(3, 50);
        this.calls.setRequestBatchSize(3, 5);

        assertFalse(this.calls.isRequestValid(3));
        assertEquals(0, this.calls.getRequestQuantity(3));
    }

    @Test
    void aQuantityOfZeroClearsTheRow() {
        this.calls.setRequestItem(1, new ItemStack(Items.DIAMOND), 10L, null);
        this.calls.setRequestQuantity(1, 0);

        assertFalse(this.calls.isRequestValid(1));
    }

    @Test
    void clearingWithoutAnIndexClearsEveryRow() {
        this.calls.setRequestItem(0, new ItemStack(Items.DIAMOND), 10L, null);
        this.calls.setRequestFluid(4, "water", 1000L, null);
        this.calls.clearAll();

        for (int row = 0; row < TileLevelMaintainer.ROWS; row++) {
            assertFalse(this.calls.isRequestValid(row));
        }
    }

    @Test
    void aFluidRowIsNoItem() {
        this.calls.setRequestFluid(1, "water", 8000L, 1000L);

        assertTrue(this.calls.isRequestValid(1));
        assertTrue(this.calls.getRequestItem(1).isEmpty());
        assertEquals(AEFluidKey.of(FluidRegistry.WATER), this.tile.keyOf(1));
        assertEquals(8000, this.calls.getRequestQuantity(1));

        final Map<String, Object> request = this.calls.getRequest(1);
        assertNotNull(request);
        assertEquals(AEKeyTypes.FLUIDS_ID.toString(), request.get("type"));
        assertEquals(8000L, request.get("quantity"));
        assertEquals(1000L, request.get("batchSize"));
        assertEquals(true, request.get("enabled"));
        assertEquals("none", request.get("state"));
    }

    @Test
    void theKeyARowGivesCopiesItToAnotherRow() {
        this.calls.setRequestFluid(0, "lava", 2000L, null);
        this.calls.setRequestItem(1, new ItemStack(Items.DIAMOND), 3L, null);

        this.calls.setRequestKey(3, (String) this.calls.getRequest(0).get("key"), 500L, null);
        this.calls.setRequestKey(4, (String) this.calls.getRequest(1).get("key"), null, null);

        assertEquals(this.tile.keyOf(0), this.tile.keyOf(3));
        assertEquals(500, this.calls.getRequestQuantity(3));
        assertEquals(this.tile.keyOf(1), this.tile.keyOf(4));
    }

    @Test
    void aRowCanBeSwitchedOff() {
        this.calls.setRequestItem(0, new ItemStack(Items.DIAMOND), 10L, null);
        this.calls.setRequestEnabled(0, false);

        assertFalse(this.calls.isRequestEnabled(0));
        assertEquals(false, this.calls.getRequest(0).get("enabled"));
        assertTrue(this.calls.isRequestValid(0));
    }

    @Test
    void badArgumentsSaySo() {
        assertThrows(IllegalArgumentException.class, () -> this.calls.isRequestValid(5));
        assertThrows(IllegalArgumentException.class, () -> this.calls.getRequest(-1));
        assertThrows(IllegalArgumentException.class, () -> this.calls.setRequestFluid(0, "no_such_fluid", 1L, null));
        assertThrows(IllegalArgumentException.class, () -> this.calls.setRequestKey(0, "{not a key", 1L, null));
        assertThrows(IllegalArgumentException.class, () -> this.calls.setRequestKey(0, "{t:\"nope:nope\"}", 1L, null));
        assertNull(this.calls.getRequest(0));
    }
}
