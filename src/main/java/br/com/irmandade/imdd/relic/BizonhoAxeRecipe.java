package br.com.irmandade.imdd.relic;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;

/**
 * Machado do Bizonho: receita do machado de diamante (3 diamantes + 2 gravetos), mas um dos gravetos
 * é o graveto da Relíquia do Bizonho e o outro é um graveto normal.
 */
public class BizonhoAxeRecipe extends SpecialCraftingRecipe {

    public BizonhoAxeRecipe(CraftingRecipeCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingRecipeInput input, World world) {
        return match(input);
    }

    @Override
    public ItemStack craft(CraftingRecipeInput input, RegistryWrapper.WrapperLookup registries) {
        return match(input) ? RelicRegistry.copyOf("bizonho_machado") : ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<? extends SpecialCraftingRecipe> getSerializer() {
        return ModRecipes.BIZONHO_AXE;
    }

    private static boolean match(CraftingRecipeInput in) {
        if (in.getWidth() != 2 || in.getHeight() != 3) return false;
        ItemStack[] s = new ItemStack[6];
        for (int i = 0; i < 6; i++) s[i] = in.getStackInSlot(i);
        // Formato do machado:  D D / D S / _ S   ou espelhado:  D D / S D / S _
        boolean normal = diamonds(s[0], s[1], s[2]) && s[4].isEmpty() && sticks(s[3], s[5]);
        boolean mirrored = diamonds(s[0], s[1], s[3]) && s[5].isEmpty() && sticks(s[2], s[4]);
        return normal || mirrored;
    }

    private static boolean diamonds(ItemStack a, ItemStack b, ItemStack c) {
        return a.isOf(Items.DIAMOND) && b.isOf(Items.DIAMOND) && c.isOf(Items.DIAMOND);
    }

    /** Um graveto da relíquia do Bizonho + um graveto normal. */
    private static boolean sticks(ItemStack a, ItemStack b) {
        if (!a.isOf(Items.STICK) || !b.isOf(Items.STICK)) return false;
        boolean aRelic = RelicRegistry.is(a, "bizonho"), bRelic = RelicRegistry.is(b, "bizonho");
        boolean aPlain = !RelicRegistry.isRelic(a), bPlain = !RelicRegistry.isRelic(b);
        return (aRelic && bPlain) || (bRelic && aPlain);
    }
}
