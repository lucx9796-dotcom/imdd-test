package br.com.irmandade.imdd.client;

import br.com.irmandade.imdd.Imdd;
import br.com.irmandade.imdd.relic.ModBlocks;
import net.minecraft.block.Block;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Marca o Magma Congelado como translúcido. Usa reflexão para funcionar com a API de camadas do Fabric
 * independente do nome da classe/enum de camada nesta versão.
 */
public final class TranslucentBlocks {
    private TranslucentBlocks() {}

    public static void register() {
        String[] names = {
                "net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap",
                "net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap"
        };
        for (String name : names) {
            try {
                Class<?> map = Class.forName(name);
                Object instance = null;
                try { instance = map.getField("INSTANCE").get(null); } catch (Exception ignored) {}
                for (Method m : map.getMethods()) {
                    if (!m.getName().equals("putBlock") || m.getParameterCount() != 2) continue;
                    if (m.getParameterTypes()[0] != Block.class) continue;
                    Class<?> layerType = m.getParameterTypes()[1];
                    Object translucent = null;
                    if (layerType.isEnum()) {
                        for (Object c : layerType.getEnumConstants()) {
                            if (((Enum<?>) c).name().equals("TRANSLUCENT")) translucent = c;
                        }
                    }
                    if (translucent == null) continue;
                    m.invoke(Modifier.isStatic(m.getModifiers()) ? null : instance, ModBlocks.FROSTED_MAGMA, translucent);
                    Imdd.LOGGER.info("Magma Congelado registrado como translúcido.");
                    return;
                }
            } catch (ClassNotFoundException ignored) {
            } catch (Exception e) {
                Imdd.LOGGER.warn("Falha ao registrar camada translúcida: {}", e.toString());
            }
        }
        Imdd.LOGGER.warn("Não consegui deixar o Magma Congelado translúcido (API de camada não encontrada).");
    }
}
