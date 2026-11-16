package br.com.irmandade.imdd.relic;

import br.com.irmandade.imdd.Imdd;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public final class ModBlocks {
    public static Block FROSTED_MAGMA;

    private ModBlocks() {}

    public static void register() {
        Identifier id = Identifier.of(Imdd.MOD_ID, "frosted_magma");
        RegistryKey<Block> key = RegistryKey.of(RegistryKeys.BLOCK, id);
        FROSTED_MAGMA = Registry.register(Registries.BLOCK, id,
                new FrostedMagmaBlock(AbstractBlock.Settings.copy(Blocks.MAGMA_BLOCK).registryKey(key).nonOpaque()));
    }
}
