package br.com.irmandade.imdd.vanish;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.List;

/** Servidor -> cliente (só para quem NÃO é op): jogadores desaparecidos, cada item no formato "uuid|nome". */
public record VanishPayload(List<String> entries) implements CustomPayload {
    public static final CustomPayload.Id<VanishPayload> ID = new CustomPayload.Id<>(Identifier.of("imdd", "vanish"));
    public static final PacketCodec<RegistryByteBuf, VanishPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.STRING.collect(PacketCodecs.toList()), VanishPayload::entries, VanishPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
