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
 * A recipe of up to three ingredients that go in in any order, one of each, and come out as one stack.
 * <p>
 * The order the machine's slots are filled in never matters, but every slot the recipe does not name has to
 * be empty: two ingredients and something else lying in the third slot is not this recipe.
 */
public class TriItemRecipe {

    public static final int SLOTS = 3;

    private final List<Ingredient> inputs;
    private final ItemStack output;

    public TriItemRecipe(final List<Ingredient> inputs, final ItemStack output) {
        this.inputs = Collections.unmodifiableList(new ArrayList<>(inputs));
        this.output = output;
    }

    public List<Ingredient> getInputs() {
        return this.inputs;
    }

    public ItemStack getOutput() {
        return this.output;
    }

    public boolean matches(final List<ItemStack> slots) {
        return this.inputs.size() <= slots.size() && this.match(0, new ArrayList<>(slots));
    }

    private boolean match(final int index, final List<ItemStack> left) {
        if (index == this.inputs.size()) {
            for (final ItemStack rest : left) {
                if (!rest.isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        for (int slot = 0; slot < left.size(); slot++) {
            final ItemStack candidate = left.get(slot);
            if (candidate.isEmpty() || !this.inputs.get(index).apply(candidate)) {
                continue;
            }
            final List<ItemStack> without = new ArrayList<>(left);
            without.remove(slot);
            if (this.match(index + 1, without)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether the output slot can take what this recipe makes.
     */
    public boolean fits(final ItemStack outputSlot) {
        return outputSlot.isEmpty()
                || ItemHandlerHelper.canItemStacksStack(outputSlot, this.output)
                        && outputSlot.getCount() + this.output.getCount() <= outputSlot.getMaxStackSize();
    }

    /**
     * One item off every slot that holds something, which is every slot this recipe named.
     */
    public static List<ItemStack> consume(final List<ItemStack> slots) {
        final ItemStack[] left = slots.toArray(new ItemStack[0]);
        for (int slot = 0; slot < left.length; slot++) {
            if (!left[slot].isEmpty()) {
                final ItemStack reduced = left[slot].copy();
                reduced.shrink(1);
                left[slot] = reduced.isEmpty() ? ItemStack.EMPTY : reduced;
            }
        }
        return Arrays.asList(left);
    }
}
