/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.integration.jei;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;

import net.minecraft.item.ItemStack;

import lazyae2.block.BlockMachine;
import lazyae2.core.Registration;
import lazyae2.recipe.AggregatorRecipe;
import lazyae2.recipe.LazyRecipes;
import lazyae2.recipe.PurifyRecipe;

@JEIPlugin
public final class LazyAE2JeiPlugin implements IModPlugin {

    @Override
    public void registerCategories(final IRecipeCategoryRegistration registry) {
        final ItemStack aggregator = machineStack(BlockMachine.Type.AGGREGATOR);
        if (aggregator != null) {
            registry.addRecipeCategories(new AggregatorCategory(registry.getJeiHelpers().getGuiHelper(), aggregator));
        }

        final ItemStack centrifuge = machineStack(BlockMachine.Type.CENTRIFUGE);
        if (centrifuge != null) {
            registry.addRecipeCategories(new CentrifugeCategory(registry.getJeiHelpers().getGuiHelper(), centrifuge));
        }
    }

    @Override
    public void register(final IModRegistry registry) {
        final ItemStack aggregator = machineStack(BlockMachine.Type.AGGREGATOR);
        if (aggregator != null) {
            final List<MachineRecipe> recipes = new ArrayList<>();
            for (final AggregatorRecipe recipe : LazyRecipes.aggregator()) {
                recipes.add(new MachineRecipe(recipe.getInputs(), recipe.getOutput()));
            }
            registry.addRecipes(recipes, AggregatorCategory.UID);
            registry.addRecipeCatalyst(aggregator, AggregatorCategory.UID);
        }

        final ItemStack centrifuge = machineStack(BlockMachine.Type.CENTRIFUGE);
        if (centrifuge != null) {
            final List<MachineRecipe> recipes = new ArrayList<>();
            for (final PurifyRecipe recipe : LazyRecipes.centrifuge()) {
                recipes.add(new MachineRecipe(recipe.getInputs(), recipe.getOutput()));
            }
            registry.addRecipes(recipes, CentrifugeCategory.UID);
            registry.addRecipeCatalyst(centrifuge, CentrifugeCategory.UID);
        }
    }

    /**
     * @return the machine as an item, or null while it is switched off in the config
     */
    @Nullable
    private static ItemStack machineStack(final BlockMachine.Type type) {
        return Registration.machine == null || !type.isEnabled() ? null
                : new ItemStack(Registration.machine, 1, type.ordinal());
    }
}
