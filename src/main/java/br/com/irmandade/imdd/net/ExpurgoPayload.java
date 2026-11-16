package br.com.irmandade.imdd.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Servidor -> cliente: tempo restante do Dia do Expurgo em ms (0 = inativo). */
public record ExpurgoPayload(long remainingMillis) implements CustomPayload {
    public static final CustomPayload.Id<ExpurgoPayload> ID = new CustomPayload.Id<>(Identifier.of("imdd", "expurgo"));
    public static final PacketCodec<RegistryByteBuf, ExpurgoPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.LONG, ExpurgoPayload::remainingMillis, ExpurgoPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
