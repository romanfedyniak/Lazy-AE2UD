/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.recipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraftforge.oredict.OreIngredient;

import lazyae2.item.ItemMaterial;
import appeng.api.AEApi;
import appeng.api.definitions.IItemDefinition;
import appeng.api.definitions.IMaterials;

/**
 * Every recipe this mod's own machines run. The lists stay open: CraftTweaker and GroovyScript edit them.
 */
public final class LazyRecipes {

    /** What charging one certus quartz crystal costs, as the old mod priced it. */
    private static final int CHARGE_CERTUS = 12000;

    private static final List<AggregatorRecipe> AGGREGATOR = new ArrayList<>();
    private static final List<PurifyRecipe> CENTRIFUGE = new ArrayList<>();
    private static final List<EtchRecipe> ETCHER = new ArrayList<>();
    private static final List<EnergizeRecipe> ENERGIZER = new ArrayList<>();

    private LazyRecipes() {
    }

    public static List<AggregatorRecipe> aggregator() {
        return AGGREGATOR;
    }

    public static List<PurifyRecipe> centrifuge() {
        return CENTRIFUGE;
    }

    public static List<EtchRecipe> etcher() {
        return ETCHER;
    }

    public static List<EnergizeRecipe> energizer() {
        return ENERGIZER;
    }

    @Nullable
    public static PurifyRecipe findCentrifuge(final ItemStack stack) {
        for (final PurifyRecipe recipe : CENTRIFUGE) {
            if (recipe.matches(stack)) {
                return recipe;
            }
        }
        return null;
    }

    @Nullable
    public static AggregatorRecipe findAggregator(final List<ItemStack> slots) {
        for (final AggregatorRecipe recipe : AGGREGATOR) {
            if (recipe.matches(slots)) {
                return recipe;
            }
        }
        return null;
    }

    @Nullable
    public static EtchRecipe findEtcher(final List<ItemStack> slots) {
        for (final EtchRecipe recipe : ETCHER) {
            if (recipe.matches(slots)) {
                return recipe;
            }
        }
        return null;
    }

    /**
     * Whether any etching recipe reads that item out of that slot, which is what the slot lets in.
     */
    public static boolean isEtchable(final int slot, final ItemStack stack) {
        for (final EtchRecipe recipe : ETCHER) {
            if (recipe.accepts(slot, stack)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static EnergizeRecipe findEnergizer(final ItemStack stack) {
        for (final EnergizeRecipe recipe : ENERGIZER) {
            if (recipe.matches(stack)) {
                return recipe;
            }
        }
        return null;
    }

    public static void registerDefaults() {
        final IMaterials materials = AEApi.instance().definitions().materials();

        // Steelmaking, the half that needs a machine
        add(Arrays.asList(ore("dustCoal"), ore("dustFluix"), ore("ingotIron")),
                ItemMaterial.Type.FLUIX_STEEL.newStack(1));
        add(Arrays.asList(ore("dustCoal"), ore("dustFluix"), ore("itemSilicon")),
                ItemMaterial.Type.STEEL_PROCESS_DUST.newStack(1));

        // The in-world recipes the machine exists to replace
        materials.fluixCrystal().maybeStack(2).ifPresent(fluix ->
                materials.certusQuartzCrystalCharged().maybeStack(1).ifPresent(charged ->
                        add(Arrays.asList(ore("gemQuartz"), ore("dustRedstone"), exact(charged)), fluix)));
        materials.skyDust().maybeStack(1).ifPresent(skyDust ->
                add(Arrays.asList(ore("gemDiamond"), exact(skyDust), ore("dustEnderPearl")),
                        ItemMaterial.Type.SPACE_GEM.newStack(1)));
        materials.skyDust().maybeStack(1).ifPresent(skyDust ->
                materials.matterBall().maybeStack(1).ifPresent(matterBall ->
                        add(Arrays.asList(exact(skyDust), exact(matterBall),
                                exact(ItemMaterial.Type.STEEL_PROCESS_DUST.newStack(1))),
                                ItemMaterial.Type.SPEC_CORE.newStack(1))));

        registerCentrifuge(materials);
        registerEtcher(materials);

        // Crystal charging, the one thing the energizer does
        materials.certusQuartzCrystalCharged().maybeStack(1).ifPresent(charged ->
                ENERGIZER.add(new EnergizeRecipe(ore("crystalCertusQuartz"), CHARGE_CERTUS, charged)));
    }

    /**
     * The processors the etcher prints, each pressed between redstone and silicon.
     */
    private static void registerEtcher(final IMaterials materials) {
        etch(ore("ingotGold"), materials.logicProcessor());
        etch(ore("crystalPureCertusQuartz"), materials.calcProcessor());
        etch(ore("gemDiamond"), materials.engProcessor());
        ETCHER.add(new EtchRecipe(ore("dustRedstone"), ore("itemSilicon"),
                exact(ItemMaterial.Type.SPACE_GEM.newStack(1)), ItemMaterial.Type.PARALLEL_PROCESSOR.newStack(1)));
        ETCHER.add(new EtchRecipe(ore("dustRedstone"), ore("itemSilicon"),
                exact(ItemMaterial.Type.SPEC_CORE_64.newStack(1)), ItemMaterial.Type.SPEC_PROCESSOR.newStack(1)));
    }

    private static void etch(final Ingredient material, final IItemDefinition output) {
        output.maybeStack(1).ifPresent(made ->
                ETCHER.add(new EtchRecipe(ore("dustRedstone"), ore("itemSilicon"), material, made)));
    }

    /**
     * The crystals the centrifuge purifies, and the three things the old mod let it grind besides.
     */
    private static void registerCentrifuge(final IMaterials materials) {
        purify(materials.certusQuartzCrystal(), materials.purifiedCertusQuartzCrystal(), 2);
        purify(materials.fluixCrystal(), materials.purifiedFluixCrystal(), 2);
        materials.purifiedNetherQuartzCrystal().maybeStack(2).ifPresent(purified ->
                CENTRIFUGE.add(new PurifyRecipe(Ingredient.fromItem(Items.QUARTZ), purified)));

        AEApi.instance().definitions().blocks().skyStoneBlock().maybeStack(1).ifPresent(skyStone ->
                materials.skyDust().maybeStack(1).ifPresent(dust ->
                        CENTRIFUGE.add(new PurifyRecipe(exact(skyStone), dust))));
        materials.enderDust().maybeStack(1).ifPresent(dust ->
                CENTRIFUGE.add(new PurifyRecipe(ore("enderpearl"), dust)));
        materials.flour().maybeStack(1).ifPresent(flour ->
                CENTRIFUGE.add(new PurifyRecipe(ore("cropWheat"), flour)));
    }

    private static void purify(final IItemDefinition crystal, final IItemDefinition purified, final int count) {
        crystal.maybeStack(1).ifPresent(from ->
                purified.maybeStack(count).ifPresent(to -> CENTRIFUGE.add(new PurifyRecipe(exact(from), to))));
    }

    private static void add(final List<Ingredient> inputs, final ItemStack output) {
        if (!output.isEmpty()) {
            AGGREGATOR.add(new AggregatorRecipe(inputs, output));
        }
    }

    private static Ingredient ore(final String name) {
        return new OreIngredient(name);
    }

    /**
     * One item and nothing else, damage included - the old mod matched its own materials this way.
     */
    private static Ingredient exact(final ItemStack stack) {
        return stack.isEmpty() ? Ingredient.EMPTY : Ingredient.fromStacks(stack);
    }
}
