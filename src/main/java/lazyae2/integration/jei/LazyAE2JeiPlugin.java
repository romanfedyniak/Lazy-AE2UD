/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.integration.jei;

import java.util.ArrayList;
import java.util.List;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;

import net.minecraft.item.ItemStack;

import lazyae2.block.BlockMachine;
import lazyae2.core.Registration;
import lazyae2.recipe.AggregatorRecipe;
import lazyae2.recipe.LazyRecipes;

@JEIPlugin
public final class LazyAE2JeiPlugin implements IModPlugin {

    @Override
    public void registerCategories(final IRecipeCategoryRegistration registry) {
        if (machineStack(BlockMachine.Type.AGGREGATOR) != null) {
            registry.addRecipeCategories(
                    new AggregatorCategory(registry.getJeiHelpers().getGuiHelper(), machineStack(BlockMachine.Type.AGGREGATOR)));
        }
    }

    @Override
    public void register(final IModRegistry registry) {
        final ItemStack aggregator = machineStack(BlockMachine.Type.AGGREGATOR);
        if (aggregator == null) {
            return;
        }

        final List<AggregatorRecipeWrapper> recipes = new ArrayList<>();
        for (final AggregatorRecipe recipe : LazyRecipes.aggregator()) {
            recipes.add(new AggregatorRecipeWrapper(recipe));
        }
        registry.addRecipes(recipes, AggregatorCategory.UID);
        registry.addRecipeCatalyst(aggregator, AggregatorCategory.UID);
    }

    /**
     * @return the machine as an item, or null while it is switched off in the config
     */
    private static ItemStack machineStack(final BlockMachine.Type type) {
        return Registration.machine == null || !type.isEnabled() ? null
                : new ItemStack(Registration.machine, 1, type.ordinal());
    }
}
