package br.com.irmandade.imdd.relic;

import br.com.irmandade.imdd.Imdd;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

/** Efeito "Espião": -40% de escala por nível (nível 2 => escala 0.2). Textura: textures/mob_effect/spy.png */
public class SpyEffect extends StatusEffect {
    public static RegistryEntry<StatusEffect> SPY;

    public SpyEffect() {
        super(StatusEffectCategory.NEUTRAL, 0xE09451);
        addAttributeModifier(EntityAttributes.SCALE, Identifier.of(Imdd.MOD_ID, "effect.spy"),
                -0.4, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }

    public static void register() {
        SPY = Registry.registerReference(Registries.STATUS_EFFECT, Identifier.of(Imdd.MOD_ID, "spy"), new SpyEffect());
    }
}
