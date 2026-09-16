/*
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.tile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.file.Files;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.FMLInjectionData;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.registries.RegistryBuilder;

import lazyae2.core.LazyAE2Config;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.AEKeyTypes;
import appeng.core.api.AEItemKeyType;

/**
 * A chamber the old mod saved, written out by hand in the shape its library gave it, read into this one.
 */
final class AssemblerMigrationTest {

    @BeforeAll
    static void setUp() throws Exception {
        Bootstrap.register();
        final File config = Files.createTempDirectory("lazyae2-config").toFile();
        config.deleteOnExit();
        // A config file is placed relative to the game directory, which only FML's launch sets
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

        // A tile writes the id it was registered under, and nothing registers these outside a game
        TileEntity.register("threng:TileBigAssemblerCore", TileAssemblerController.class);
        TileEntity.register("threng:TileBigAssemblerPart", TileAssemblerPart.class);
        TileEntity.register("threng:TileBigAssemblerPatternStore", TileAssemblerPatterns.class);
    }

    /** AE2's {@code Platform} reads the side in a static initialiser, and there is no FML around to answer. */
    private static void fakeServerSide() throws Exception {
        final Class<?> handlerType = Class.forName("net.minecraftforge.fml.common.IFMLSidedHandler");
        final Object delegate = Proxy.newProxyInstance(handlerType.getClassLoader(), new Class<?>[] { handlerType },
                (proxy, method, args) -> "getSide".equals(method.getName()) ? Side.SERVER : null);
        final Field field = FMLCommonHandler.class.getDeclaredField("sidedDelegate");
        field.setAccessible(true);
        field.set(FMLCommonHandler.instance(), delegate);
    }

    @Test
    void aBlockRemembersTheControllerItWasAssembledBy() {
        final NBTTagCompound core = new NBTTagCompound();
        core.setInteger("x", 10);
        core.setInteger("y", 64);
        core.setInteger("z", -3);
        final NBTTagCompound multiBlock = new NBTTagCompound();
        multiBlock.setTag("ChildDirs", new NBTTagCompound());
        multiBlock.setTag("CorePos", core);
        final NBTTagCompound saved = tile("threng:TileBigAssemblerPart");
        saved.setTag("MultiBlock", multiBlock);

        final TileAssemblerPart part = new TileAssemblerPart();
        part.readFromNBT(saved);

        assertEquals(new BlockPos(10, 64, -3), part.getControllerPos());
        assertTrue(part.isAssembled());

        final TileAssemblerPart again = new TileAssemblerPart();
        again.readFromNBT(part.writeToNBT(new NBTTagCompound()));
        assertEquals(new BlockPos(10, 64, -3), again.getControllerPos());
    }

    @Test
    void aBlockTheOldChamberNeverReachedIsNotAssembled() {
        final NBTTagCompound saved = tile("threng:TileBigAssemblerPart");
        final NBTTagCompound multiBlock = new NBTTagCompound();
        multiBlock.setTag("ChildDirs", new NBTTagCompound());
        saved.setTag("MultiBlock", multiBlock);

        final TileAssemblerPart part = new TileAssemblerPart();
        part.readFromNBT(saved);

        assertNull(part.getControllerPos());
        assertFalse(part.isAssembled());
    }

    @Test
    void aPatternModuleKeepsEachPatternInItsSlot() {
        final NBTTagList items = new NBTTagList();
        for (int slot = 0; slot < TileAssemblerPatterns.SLOTS; slot++) {
            final NBTTagCompound item = new NBTTagCompound();
            if (slot == 0 || slot == 35) {
                new ItemStack(Items.PAPER, 1).writeToNBT(item);
            }
            items.appendTag(item);
        }
        final NBTTagCompound inventory = new NBTTagCompound();
        inventory.setTag("Items", items);
        final NBTTagCompound saved = tile("threng:TileBigAssemblerPatternStore");
        saved.setTag("PatternInv", inventory);

        final TileAssemblerPatterns module = new TileAssemblerPatterns();
        module.readFromNBT(saved);

        assertEquals(Items.PAPER, module.getPatterns().getStackInSlot(0).getItem());
        assertEquals(Items.PAPER, module.getPatterns().getStackInSlot(35).getItem());
        assertTrue(module.getPatterns().getStackInSlot(1).isEmpty());

        final TileAssemblerPatterns again = new TileAssemblerPatterns();
        again.readFromNBT(module.writeToNBT(new NBTTagCompound()));
        assertEquals(Items.PAPER, again.getPatterns().getStackInSlot(35).getItem());
    }

    @Test
    void anAssembledControllerIsAssembledAgainAndKeepsItsWork() {
        final NBTTagCompound multiBlock = new NBTTagCompound();
        multiBlock.setTag("ChildDirs", new NBTTagCompound());
        multiBlock.setBoolean("Formed", true);

        final NBTTagList jobs = new NBTTagList();
        jobs.appendTag(job(new ItemStack(Items.CAKE), new ItemStack(Items.BUCKET, 3)));
        jobs.appendTag(job(new ItemStack(Items.STICK, 4)));
        final NBTTagCompound queue = new NBTTagCompound();
        queue.setByteArray("JobSlots", new byte[] { 3 });
        queue.setTag("Queue", jobs);

        final NBTTagList buffer = new NBTTagList();
        buffer.appendTag(new NBTTagCompound());
        buffer.appendTag(new ItemStack(Items.DIAMOND, 2).writeToNBT(new NBTTagCompound()));
        final NBTTagCompound outputs = new NBTTagCompound();
        outputs.setTag("Items", buffer);

        final NBTTagCompound saved = tile("threng:TileBigAssemblerCore");
        saved.setTag("MultiBlock", multiBlock);
        saved.setTag("JobQueue", queue);
        saved.setTag("OutputBuffer", outputs);
        saved.setTag("CraftingBuffer", new NBTTagCompound());
        saved.setInteger("CpuCount", 3);
        saved.setInteger("Work", 7);
        saved.setTag("aeproxy", new NBTTagCompound());

        final TileAssemblerController controller = new TileAssemblerController();
        controller.readFromNBT(saved);
        final NBTTagCompound written = controller.writeToNBT(new NBTTagCompound());

        // Its walls are found again once the world around it has loaded; until then it is not assembled
        assertFalse(controller.isAssembled());
        assertTrue(written.getBoolean("legacyAssembled"));

        final NBTTagList work = written.getTagList("work", 10);
        assertEquals(3, work.tagCount(), "two jobs and the output buffer");
        assertEquals(1, work.getCompoundTagAt(0).getInteger("copies"));
        assertEquals(0, work.getCompoundTagAt(0).getDouble("progress"), 1e-9, "a queued job starts over");
        assertEquals(2, work.getCompoundTagAt(0).getTagList("outputs", 10).tagCount(), "the cake and the buckets");
        assertEquals(0, work.getCompoundTagAt(2).getInteger("copies"), "what was already made holds no slot");
        assertEquals(1, work.getCompoundTagAt(2).getDouble("progress"), 1e-9);

        final TileAssemblerController again = new TileAssemblerController();
        again.readFromNBT(written);
        assertEquals(3, again.writeToNBT(new NBTTagCompound()).getTagList("work", 10).tagCount());
    }

    /** One job of the old queue: what it makes, and the nine slots of what it leaves behind. */
    private static NBTTagCompound job(final ItemStack result, final ItemStack... left) {
        final NBTTagList remaining = new NBTTagList();
        for (int slot = 0; slot < 9; slot++) {
            remaining.appendTag((slot < left.length ? left[slot] : ItemStack.EMPTY).writeToNBT(new NBTTagCompound()));
        }
        final NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("Index", 0);
        tag.setTag("Remaining", remaining);
        tag.setTag("Result", result.writeToNBT(new NBTTagCompound()));
        return tag;
    }

    private static NBTTagCompound tile(final String id) {
        final NBTTagCompound tag = new NBTTagCompound();
        tag.setString("id", id);
        tag.setInteger("x", 0);
        tag.setInteger("y", 0);
        tag.setInteger("z", 0);
        return tag;
    }
}
