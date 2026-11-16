package br.com.irmandade.imdd.mixin.client;

import br.com.irmandade.imdd.client.VanishClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Painel de report / interações sociais: ele lê os UUIDs de ClientPlayNetworkHandler (class_634, method_31363).
 * Quem está desaparecido sai dessa lista.
 */
@Pseudo
@Mixin(targets = "net.minecraft.class_634")
public abstract class ClientPlayNetworkHandlerMixin {

    @Inject(method = "method_31363", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void imdd$esconderUuids(CallbackInfoReturnable<Collection<UUID>> cir) {
        Set<UUID> hidden = VanishClient.uuids();
        if (hidden.isEmpty()) return;
        List<UUID> out = new ArrayList<>();
        for (UUID id : cir.getReturnValue()) if (!hidden.contains(id)) out.add(id);
        cir.setReturnValue(out);
    }
}
