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
import java.util.Locale;
import java.util.Set;

/** Sugestões de nome no chat (/tell etc.): o cliente monta a lista sozinho, então filtra os desaparecidos aqui. */
@Pseudo
@Mixin(targets = "net.minecraft.class_637")
public abstract class ClientCommandSourceMixin {

    @Inject(method = "method_9262", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void imdd$esconderNomes(CallbackInfoReturnable<Collection<String>> cir) {
        Set<String> hidden = VanishClient.names();
        if (hidden.isEmpty()) return;
        List<String> out = new ArrayList<>();
        for (String n : cir.getReturnValue()) if (!hidden.contains(n.toLowerCase(Locale.ROOT))) out.add(n);
        cir.setReturnValue(out);
    }
}
