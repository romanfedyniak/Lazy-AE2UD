/*
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.integration.groovyscript;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.item.crafting.Ingredient;

import com.cleanroommc.groovyscript.api.GroovyLog;
import com.cleanroommc.groovyscript.api.IIngredient;
import com.cleanroommc.groovyscript.api.documentation.annotations.Comp;
import com.cleanroommc.groovyscript.api.documentation.annotations.Example;
import com.cleanroommc.groovyscript.api.documentation.annotations.MethodDescription;
import com.cleanroommc.groovyscript.api.documentation.annotations.Property;
import com.cleanroommc.groovyscript.api.documentation.annotations.RecipeBuilderDescription;
import com.cleanroommc.groovyscript.api.documentation.annotations.RecipeBuilderRegistrationMethod;
import com.cleanroommc.groovyscript.api.documentation.annotations.RegistryDescription;
import com.cleanroommc.groovyscript.helper.recipe.AbstractRecipeBuilder;
import com.cleanroommc.groovyscript.registry.StandardListRegistry;

import lazyae2.recipe.AggregatorRecipe;
import lazyae2.recipe.LazyRecipes;

@RegistryDescription
public final class Aggregator extends StandardListRegistry<AggregatorRecipe> {

    @RecipeBuilderDescription(example = {
            @Example(".input(ore('blockGlass'), item('minecraft:diamond')).output(item('minecraft:diamond') * 4)"),
            @Example(".input(item('minecraft:gold_ingot')).output(item('minecraft:diamond'))")
    })
    public RecipeBuilder recipeBuilder() {
        return new RecipeBuilder(this);
    }

    @Override
    public Collection<AggregatorRecipe> getRecipes() {
        return LazyRecipes.aggregator();
    }

    @MethodDescription(example = @Example("item('appliedenergistics2:material:45')"))
    public void removeByInput(final IIngredient input) {
        this.getRecipes().removeIf(recipe -> {
            for (final Ingredient each : recipe.getInputs()) {
                if (ScriptIngredient.anyMatches(input, each)) {
                    this.addBackup(recipe);
                    return true;
                }
            }
            return false;
        });
    }

    @MethodDescription(example = @Example("item('appliedenergistics2:material:7')"))
    public void removeByOutput(final IIngredient output) {
        this.getRecipes().removeIf(recipe -> {
            if (output.test(recipe.getOutput())) {
                this.addBackup(recipe);
                return true;
            }
            return false;
        });
    }

    @Property(property = "input", comp = @Comp(gte = 1, lte = 3))
    @Property(property = "output", comp = @Comp(eq = 1))
    public static final class RecipeBuilder extends AbstractRecipeBuilder<AggregatorRecipe> {

        private final Aggregator registry;

        RecipeBuilder(final Aggregator registry) {
            this.registry = registry;
        }

        @Override
        public String getErrorMsg() {
            return "Error adding Lazy AE2 Aggregator recipe";
        }

        @Override
        protected int getMaxItemInput() {
            return 1;
        }

        @Override
        public void validate(final GroovyLog.Msg msg) {
            this.validateItems(msg, 1, 3, 1, 1);
            this.validateFluids(msg);
        }

        @Override
        @RecipeBuilderRegistrationMethod
        public @Nullable AggregatorRecipe register() {
            if (!this.validate()) {
                return null;
            }
            final List<Ingredient> inputs = new ArrayList<>();
            for (final IIngredient each : this.input) {
                inputs.add(ScriptIngredient.of(each));
            }
            final AggregatorRecipe recipe = new AggregatorRecipe(inputs, this.output.get(0));
            this.registry.add(recipe);
            return recipe;
        }
    }
}
