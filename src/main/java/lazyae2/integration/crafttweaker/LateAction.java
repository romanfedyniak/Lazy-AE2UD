/*
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.integration.crafttweaker;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import net.minecraft.item.ItemStack;

import crafttweaker.CraftTweakerAPI;
import crafttweaker.IAction;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import crafttweaker.mc1120.CraftTweaker;

/**
 * A script's change to a machine's recipes, held until CraftTweaker's post-init. Scripts run before this mod
 * registers its own recipes, so a removal done at once would find nothing to remove.
 */
final class LateAction implements IAction {

    private final Runnable change;
    private final String description;

    private LateAction(final Runnable change, final String description) {
        this.change = change;
        this.description = description;
    }

    static void queue(final Runnable change, final String description) {
        CraftTweaker.LATE_ACTIONS.add(new LateAction(change, description));
    }

    static <R> void add(final List<R> recipes, final Supplier<R> recipe, final String machine, final IItemStack output) {
        queue(() -> recipes.add(recipe.get()), "Adding " + machine + " recipe for " + output);
    }

    static <R> void remove(final List<R> recipes, final Function<R, ItemStack> outputOf,
            final String machine, final IItemStack output) {
        queue(() -> {
            final Predicate<R> makes = recipe -> output.matches(CraftTweakerMC.getIItemStack(outputOf.apply(recipe)));
            if (!recipes.removeIf(makes)) {
                CraftTweakerAPI.logWarning("No " + machine + " recipes were removed for " + output);
            }
        }, "Removing " + machine + " recipes for " + output);
    }

    @Override
    public void apply() {
        this.change.run();
    }

    @Override
    public String describe() {
        return this.description;
    }
}
