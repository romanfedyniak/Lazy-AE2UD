/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.recipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * What the ME Circuit Etcher makes: a processor etched out of what lies between the two presses.
 * <p>
 * Unlike the Fluix Aggregator, each slot means something of its own - the etching agent on top, the wafer
 * below and the material between them - so an ingredient matches its own slot and no other.
 */
public class EtchRecipe {

    public static final int SLOTS = 3;
    /** The slot the material goes in; the two others hold what etches it. */
    public static final int MATERIAL = 2;

    private final List<Ingredient> inputs;
    private final ItemStack output;

    public EtchRecipe(final Ingredient top, final Ingredient bottom, final Ingredient material, final ItemStack output) {
        this.inputs = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(top, bottom, material)));
        this.output = output;
    }

    public List<Ingredient> getInputs() {
        return this.inputs;
    }

    public ItemStack getOutput() {
        return this.output;
    }

    public boolean matches(final List<ItemStack> slots) {
        for (int slot = 0; slot < SLOTS; slot++) {
            if (!this.inputs.get(slot).apply(slots.get(slot))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether this recipe reads the given item out of that slot.
     */
    public boolean accepts(final int slot, final ItemStack stack) {
        return !stack.isEmpty() && this.inputs.get(slot).apply(stack);
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
