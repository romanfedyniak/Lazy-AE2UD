/*
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;

/**
 * Which slots of a machine a recipe reads, and what is left of them afterwards.
 */
final class TriItemRecipeTest {

    static {
        Bootstrap.register();
    }

    private static ItemStack stack(final Item item) {
        return new ItemStack(item);
    }

    private static List<ItemStack> slots(final ItemStack... stacks) {
        final List<ItemStack> slots = new ArrayList<>(TriItemRecipe.SLOTS);
        Collections.addAll(slots, stacks);
        while (slots.size() < TriItemRecipe.SLOTS) {
            slots.add(ItemStack.EMPTY);
        }
        return slots;
    }

    private static TriItemRecipe recipe(final Item... items) {
        final List<Ingredient> inputs = new ArrayList<>(items.length);
        for (final Item item : items) {
            inputs.add(Ingredient.fromItem(item));
        }
        return new TriItemRecipe(inputs, stack(Items.DIAMOND));
    }

    @Test
    void takesTheSlotsInAnyOrder() {
        final TriItemRecipe recipe = recipe(Items.COAL, Items.REDSTONE, Items.IRON_INGOT);
        assertTrue(recipe.matches(slots(stack(Items.COAL), stack(Items.REDSTONE), stack(Items.IRON_INGOT))));
        assertTrue(recipe.matches(slots(stack(Items.IRON_INGOT), stack(Items.COAL), stack(Items.REDSTONE))));
        assertTrue(recipe.matches(slots(stack(Items.REDSTONE), stack(Items.IRON_INGOT), stack(Items.COAL))));
    }

    @Test
    void refusesAMissingIngredient() {
        final TriItemRecipe recipe = recipe(Items.COAL, Items.REDSTONE, Items.IRON_INGOT);
        assertFalse(recipe.matches(slots(stack(Items.COAL), stack(Items.REDSTONE))));
    }

    @Test
    void refusesAnythingLyingInASlotItDoesNotName() {
        final TriItemRecipe recipe = recipe(Items.COAL, Items.REDSTONE);
        assertTrue(recipe.matches(slots(stack(Items.COAL), stack(Items.REDSTONE))));
        assertFalse(recipe.matches(slots(stack(Items.COAL), stack(Items.REDSTONE), stack(Items.IRON_INGOT))));
    }

    @Test
    void neverReadsOneSlotTwice() {
        final TriItemRecipe recipe = recipe(Items.COAL, Items.COAL);
        assertFalse(recipe.matches(slots(stack(Items.COAL))));
        assertTrue(recipe.matches(slots(stack(Items.COAL), stack(Items.COAL))));
    }

    @Test
    void takesOneItemOffEverySlotThatHoldsSomething() {
        final ItemStack three = new ItemStack(Items.COAL, 3);
        final List<ItemStack> left = TriItemRecipe.consume(slots(three, stack(Items.REDSTONE)));

        assertEquals(2, left.get(0).getCount());
        assertTrue(left.get(1).isEmpty());
        assertTrue(left.get(2).isEmpty());
        assertEquals(3, three.getCount(), "the slots handed in are left alone");
    }

    @Test
    void fitsWhatTheOutputSlotCanStillTake() {
        final TriItemRecipe recipe = new TriItemRecipe(Arrays.asList(Ingredient.fromItem(Items.COAL)), stack(Items.DIAMOND));

        assertTrue(recipe.fits(ItemStack.EMPTY));
        assertTrue(recipe.fits(new ItemStack(Items.DIAMOND, 10)));
        assertFalse(recipe.fits(new ItemStack(Items.DIAMOND, 64)));
        assertFalse(recipe.fits(stack(Items.IRON_INGOT)));
    }
}
