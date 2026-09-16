/*
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.tile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.RegistryBuilder;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.AEKeyTypes;
import appeng.api.stacks.GenericStack;
import appeng.core.api.AEItemKeyType;

/**
 * How a chamber's batches are paid for, finished and handed back.
 */
final class AssemblerWorkTest {

    private static final int TICKS = 10;
    private static final double ENERGY = 100;

    private static AEItemKey gear;
    private static AEItemKey bucket;

    @BeforeAll
    static void setUp() {
        Bootstrap.register();
        if (GameRegistry.findRegistry(AEKeyType.class) == null) {
            new RegistryBuilder<AEKeyType>().setName(AEKeyTypes.REGISTRY_NAME).setType(AEKeyType.class)
                    .setIDRange(0, 127).create();
            AEKeyTypes.register(new AEItemKeyType());
        }
        gear = AEItemKey.of(Items.GOLD_INGOT);
        bucket = AEItemKey.of(Items.BUCKET);
    }

    private static List<GenericStack> make(final AEKey what, final long amount) {
        return Collections.singletonList(new GenericStack(what, amount));
    }

    @Test
    void aFullyPaidBatchTakesTenTicksAndTenPercentAPiece() {
        final AssemblerWork work = new AssemblerWork();
        work.add(1000, make(gear, 1000));

        assertEquals(10000, work.powerWanted(TICKS, ENERGY), 1e-6, "a thousand crafts at 10 AE a tick each");
        for (int tick = 0; tick < TICKS; tick++) {
            assertFalse(work.deliver((what, amount) -> amount, tick), "nothing is done before its tenth tick");
            work.spend(work.powerWanted(TICKS, ENERGY), TICKS, ENERGY);
        }
        assertEquals(0, work.powerWanted(TICKS, ENERGY), 1e-6);

        final Map<AEKey, Long> got = new HashMap<>();
        assertTrue(work.deliver((what, amount) -> {
            got.merge(what, amount, Long::sum);
            return amount;
        }, TICKS));
        assertEquals(1000L, got.get(gear));
        assertTrue(work.isEmpty());
        assertEquals(0, work.getBusy());
    }

    @Test
    void shortPowerGoesToTheOldestBatchFirst() {
        final AssemblerWork work = new AssemblerWork();
        work.add(10, make(gear, 10));
        work.add(10, make(bucket, 10));

        // Enough for the first batch every tick and nothing more
        for (int tick = 0; tick < TICKS; tick++) {
            work.spend(100, TICKS, ENERGY);
        }

        final Map<AEKey, Long> got = new HashMap<>();
        work.deliver((what, amount) -> {
            got.put(what, amount);
            return amount;
        }, TICKS);
        assertEquals(10L, got.get(gear));
        assertFalse(got.containsKey(bucket), "the newer batch has had nothing");
        assertEquals(10, work.getBusy());
    }

    @Test
    void aBatchPaidInPartMovesOnByThatPart() {
        final AssemblerWork work = new AssemblerWork();
        work.add(10, make(gear, 10));

        for (int tick = 0; tick < 2 * TICKS; tick++) {
            work.spend(50, TICKS, ENERGY);
        }
        assertTrue(work.deliver((what, amount) -> amount, 2 * TICKS), "half the power takes twice as long");
    }

    @Test
    void whatTheNetworkRefusesStaysAndHoldsItsSlotsUntilItIsTaken() {
        final AssemblerWork work = new AssemblerWork();
        work.add(4, make(gear, 400));
        for (int tick = 0; tick < TICKS; tick++) {
            work.spend(1e9, TICKS, ENERGY);
        }

        assertTrue(work.deliver((what, amount) -> 150, 100));
        assertEquals(4, work.getBusy());
        assertFalse(work.deliver((what, amount) -> amount, 100 + AssemblerWork.RETRY_TICKS - 1),
                "a refused batch is not offered again at once");

        final long[] taken = new long[1];
        assertTrue(work.deliver((what, amount) -> taken[0] = amount, 100 + AssemblerWork.RETRY_TICKS));
        assertEquals(250, taken[0]);
        assertEquals(0, work.getBusy());
    }

    @Test
    void finishedWorkHoldsNoSlot() {
        final AssemblerWork work = new AssemblerWork();
        work.addFinished(make(gear, 64));

        assertEquals(0, work.getBusy());
        assertTrue(work.deliver((what, amount) -> amount, 0));
        assertTrue(work.isEmpty());
    }

    @Test
    void breakingTheControllerHandsBackEverythingFinishedOrNot() {
        final AssemblerWork work = new AssemblerWork();
        work.add(3, make(gear, 3));
        work.addFinished(make(bucket, 5));

        final List<GenericStack> all = work.takeAll();

        assertEquals(2, all.size());
        assertTrue(work.isEmpty());
        assertEquals(0, work.getBusy());
    }

    @Test
    void workSurvivesASave() {
        final AssemblerWork work = new AssemblerWork();
        work.add(7, make(gear, 7));
        work.spend(1e9, TICKS, ENERGY);
        work.addFinished(make(bucket, 2));

        final AssemblerWork loaded = new AssemblerWork();
        loaded.readFromNBT(work.writeToNBT());

        assertEquals(7, loaded.getBusy());
        assertEquals(work.powerWanted(TICKS, ENERGY), loaded.powerWanted(TICKS, ENERGY), 1e-9);
    }
}
