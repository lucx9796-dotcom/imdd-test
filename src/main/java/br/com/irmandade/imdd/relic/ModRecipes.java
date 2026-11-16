package br.com.irmandade.imdd.relic;

import br.com.irmandade.imdd.Imdd;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModRecipes {
    public static RecipeSerializer<BizonhoAxeRecipe> BIZONHO_AXE;

    private ModRecipes() {}

    public static void register() {
        BIZONHO_AXE = Registry.register(Registries.RECIPE_SERIALIZER, Identifier.of(Imdd.MOD_ID, "bizonho_axe"),
                new SpecialCraftingRecipe.SpecialRecipeSerializer<>(BizonhoAxeRecipe::new));
    }
}
