/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.recipe;

import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * A recipe of one thing in and one stack out, taking a single item off the slot each time it runs.
 */
public class SingleItemRecipe {

    private final Ingredient input;
    private final ItemStack output;

    public SingleItemRecipe(final Ingredient input, final ItemStack output) {
        this.input = input;
        this.output = output;
    }

    public Ingredient getInput() {
        return this.input;
    }

    public List<Ingredient> getInputs() {
        return Collections.singletonList(this.input);
    }

    public ItemStack getOutput() {
        return this.output;
    }

    public boolean matches(final ItemStack stack) {
        return !stack.isEmpty() && this.input.apply(stack);
    }

    /**
     * Whether the output slot can take what this recipe makes.
     */
    public boolean fits(final ItemStack outputSlot) {
        return outputSlot.isEmpty()
                || ItemHandlerHelper.canItemStacksStack(outputSlot, this.output)
                        && outputSlot.getCount() + this.output.getCount() <= outputSlot.getMaxStackSize();
    }
}
