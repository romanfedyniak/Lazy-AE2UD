/*
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.integration.groovyscript;

import java.util.Collection;

import org.jetbrains.annotations.Nullable;

import net.minecraft.item.crafting.Ingredient;

import com.cleanroommc.groovyscript.api.GroovyLog;
import com.cleanroommc.groovyscript.api.IIngredient;
import com.cleanroommc.groovyscript.api.documentation.annotations.Comp;
import com.cleanroommc.groovyscript.api.documentation.annotations.Example;
import com.cleanroommc.groovyscript.api.documentation.annotations.MethodDescription;
import com.cleanroommc.groovyscript.api.documentation.annotations.Property;
import com.cleanroommc.groovyscript.api.documentation.annotations.RecipeBuilderDescription;
import com.cleanroommc.groovyscript.api.documentation.annotations.RecipeBuilderMethodDescription;
import com.cleanroommc.groovyscript.api.documentation.annotations.RecipeBuilderRegistrationMethod;
import com.cleanroommc.groovyscript.api.documentation.annotations.RegistryDescription;
import com.cleanroommc.groovyscript.helper.recipe.AbstractRecipeBuilder;
import com.cleanroommc.groovyscript.registry.StandardListRegistry;

import lazyae2.recipe.EtchRecipe;
import lazyae2.recipe.LazyRecipes;

@RegistryDescription
public final class Etcher extends StandardListRegistry<EtchRecipe> {

    @RecipeBuilderDescription(example = {
            @Example(".input(ore('blockGlass')).top(item('minecraft:diamond')).bottom(item('minecraft:clay')).output(item('minecraft:diamond') * 5)"),
            @Example(".input(item('minecraft:gold_ingot')).output(item('minecraft:diamond'))")
    })
    public RecipeBuilder recipeBuilder() {
        return new RecipeBuilder(this);
    }

    @Override
    public Collection<EtchRecipe> getRecipes() {
        return LazyRecipes.etcher();
    }

    @MethodDescription(example = @Example("item('minecraft:diamond')"))
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

    @MethodDescription(example = @Example("item('appliedenergistics2:material:22')"))
    public void removeByOutput(final IIngredient output) {
        this.getRecipes().removeIf(recipe -> {
            if (output.test(recipe.getOutput())) {
                this.addBackup(recipe);
                return true;
            }
            return false;
        });
    }

    @Property(property = "input", comp = @Comp(eq = 1))
    @Property(property = "output", comp = @Comp(eq = 1))
    public static final class RecipeBuilder extends AbstractRecipeBuilder<EtchRecipe> {

        private final Etcher registry;

        /** A press left out means an empty slot, as it did in GroovyScript's own support. */
        @Property(defaultValue = "IIngredient.EMPTY")
        private IIngredient top = IIngredient.EMPTY;
        @Property(defaultValue = "IIngredient.EMPTY")
        private IIngredient bottom = IIngredient.EMPTY;

        RecipeBuilder(final Etcher registry) {
            this.registry = registry;
        }

        @RecipeBuilderMethodDescription
        public RecipeBuilder top(final IIngredient top) {
            this.top = top;
            return this;
        }

        @RecipeBuilderMethodDescription
        public RecipeBuilder bottom(final IIngredient bottom) {
            this.bottom = bottom;
            return this;
        }

        @Override
        public String getErrorMsg() {
            return "Error adding Lazy AE2 Etcher recipe";
        }

        @Override
        protected int getMaxItemInput() {
            return 1;
        }

        @Override
        public void validate(final GroovyLog.Msg msg) {
            this.validateItems(msg, 1, 1, 1, 1);
            this.validateFluids(msg);
        }

        @Override
        @RecipeBuilderRegistrationMethod
        public @Nullable EtchRecipe register() {
            if (!this.validate()) {
                return null;
            }
            final EtchRecipe recipe = new EtchRecipe(ScriptIngredient.of(this.top), ScriptIngredient.of(this.bottom),
                    ScriptIngredient.of(this.input.get(0)), this.output.get(0));
            this.registry.add(recipe);
            return recipe;
        }
    }
}
