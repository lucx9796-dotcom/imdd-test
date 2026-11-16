package br.com.irmandade.imdd.client;

import br.com.irmandade.imdd.vanish.VanishPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Lado do cliente do /desaparecer: guarda quem está desaparecido (só chega para quem não é op). */
public final class VanishClient {
    private static volatile Set<UUID> uuids = Set.of();
    private static volatile Set<String> names = Set.of();

    private VanishClient() {}

    public static Set<UUID> uuids() { return uuids; }

    /** Nomes em minúsculas. */
    public static Set<String> names() { return names; }

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(VanishPayload.ID, (payload, context) -> {
            Set<UUID> u = new HashSet<>();
            Set<String> n = new HashSet<>();
            for (String entry : payload.entries()) {
                int sep = entry.indexOf('|');
                if (sep < 0) continue;
                try {
                    u.add(UUID.fromString(entry.substring(0, sep)));
                    n.add(entry.substring(sep + 1).toLowerCase(Locale.ROOT));
                } catch (IllegalArgumentException ignored) {}
            }
            uuids = u;
            names = n;
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            uuids = Set.of();
            names = Set.of();
        });
    }
}
