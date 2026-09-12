/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.integration.jei;

import javax.annotation.Nullable;

import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import lazyae2.Tags;
import lazyae2.recipe.TriItemRecipe;

/**
 * What the Fluix Aggregator makes, as a recipe screen: three slots in, one out. The slots and the arrow are
 * cut out of the furnace, the way the categories that have no window of their own do it in AE2.
 */
final class AggregatorCategory implements IRecipeCategory<AggregatorRecipeWrapper> {

    static final String UID = Tags.MOD_ID + ".aggregator";

    private static final ResourceLocation FURNACE = new ResourceLocation("minecraft", "textures/gui/container/furnace.png");
    private static final int WIDTH = 150;
    private static final int SLOT = 18;
    private static final int ARROW_X = 68;
    private static final int ARROW_WIDTH = 24;
    private static final int OUTPUT_X = 104;
    /**
     * HEI stacks three buttons upwards from the bottom edge of a category, so one shorter than this has them
     * hanging above its own entry (RecipeLayout.setPosition).
     */
    private static final int BUTTON_COLUMN = 13 * 3 + 4;
    private static final int HEIGHT = Math.max(SLOT, BUTTON_COLUMN);
    private static final int SLOT_TOP = (HEIGHT - SLOT) / 2;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable slot;
    private final IDrawable arrow;

    AggregatorCategory(final IGuiHelper helper, final ItemStack machine) {
        this.background = helper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = helper.createDrawableIngredient(machine);
        this.slot = helper.createDrawable(FURNACE, 55, 16, SLOT, SLOT);
        this.arrow = helper.createDrawable(FURNACE, 79, 35, ARROW_WIDTH, 17);
    }

    private static int inputX(final int input) {
        return ARROW_X - 6 - (TriItemRecipe.SLOTS - input) * SLOT;
    }

    @Override
    public String getUid() {
        return UID;
    }

    @Override
    public String getTitle() {
        return I18n.format("tile.threng.machine.aggregator.name");
    }

    @Override
    public String getModName() {
        return Tags.MOD_NAME;
    }

    @Override
    public IDrawable getBackground() {
        return this.background;
    }

    @Nullable
    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void drawExtras(final Minecraft minecraft) {
        for (int input = 0; input < TriItemRecipe.SLOTS; input++) {
            this.slot.draw(minecraft, inputX(input), SLOT_TOP);
        }
        this.arrow.draw(minecraft, ARROW_X, SLOT_TOP);
        this.slot.draw(minecraft, OUTPUT_X, SLOT_TOP);
    }

    @Override
    public void setRecipe(final IRecipeLayout layout, final AggregatorRecipeWrapper recipe, final IIngredients ingredients) {
        final IGuiItemStackGroup items = layout.getItemStacks();
        for (int input = 0; input < TriItemRecipe.SLOTS; input++) {
            items.init(input, true, inputX(input), SLOT_TOP);
        }
        items.init(TriItemRecipe.SLOTS, false, OUTPUT_X, SLOT_TOP);
        items.set(ingredients);
    }
}
