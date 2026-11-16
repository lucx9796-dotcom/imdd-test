package br.com.irmandade.imdd.vanish;

import br.com.irmandade.imdd.Imdd;
import br.com.irmandade.imdd.expurgo.Expurgo;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * /desaparecer <jogador>: o jogador some das listas, mas continua existindo no mundo como entidade.
 *  - tab: some para quem não é op (pacote UPDATE_LISTED com listed=false; a entidade continua sendo desenhada);
 *  - painel de report e sugestões de nome (/tell etc.): o cliente do mod filtra (VanishPayload);
 *  - /tell e seletores (@a): o servidor filtra para quem não é op (ver mixins);
 *  - conquistas, entrar/sair e mortes dele não aparecem no chat.
 * Quem é op (nível 2) vê tudo normalmente.
 */
public final class VanishState {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<UUID, String> VANISHED = new LinkedHashMap<>(); // uuid -> nome
    private static final Map<UUID, String> SIGNATURE = new HashMap<>();      // última situação enviada a cada jogador
    private static final Predicate<ServerCommandSource> OP = CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK);

    private VanishState() {}

    // ---------- consultas ----------

    public static boolean isVanished(UUID id) { return VANISHED.containsKey(id); }

    public static Map<UUID, String> vanished() { return VANISHED; }

    public static boolean isOp(ServerPlayerEntity p) {
        try { return OP.test(p.getCommandSource()); } catch (Exception e) { return false; }
    }

    /** Console, command block e ops enxergam tudo. */
    public static boolean sourceIsOp(ServerCommandSource src) {
        ServerPlayerEntity p = src.getPlayer();
        return p == null || isOp(p);
    }

    public static <T> List<T> filterPlayers(ServerCommandSource src, List<T> list) {
        if (VANISHED.isEmpty() || sourceIsOp(src)) return list;
        ServerPlayerEntity self = src.getPlayer();
        List<T> out = new ArrayList<>();
        for (T t : list) {
            if (t instanceof ServerPlayerEntity p && VANISHED.containsKey(p.getUuid()) && (self == null || !self.getUuid().equals(p.getUuid()))) continue;
            out.add(t);
        }
        return out;
    }

    public static Collection<String> filterNames(ServerCommandSource src, Collection<String> names) {
        if (VANISHED.isEmpty() || sourceIsOp(src)) return names;
        List<String> out = new ArrayList<>();
        for (String n : names) {
            boolean hide = false;
            for (String v : VANISHED.values()) if (v.equalsIgnoreCase(n)) hide = true;
            ServerPlayerEntity self = src.getPlayer();
            if (hide && self != null && self.getName().getString().equalsIgnoreCase(n)) hide = false;
            if (!hide) out.add(n);
        }
        return out;
    }

    // ---------- registro de eventos ----------

    public static void init() {
        ServerLifecycleEvents.SERVER_STARTED.register(s -> load());
        ServerLifecycleEvents.SERVER_STOPPING.register(s -> save());
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity p = handler.getPlayer();
            if (VANISHED.containsKey(p.getUuid())) VANISHED.put(p.getUuid(), p.getName().getString());
            refresh(server, true); // o servidor acabou de mandar a lista com todos "listados": refaz o esconderijo
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 40 == 0) refresh(server, false); // detecta quem virou/deixou de ser op
        });
        // conquistas, entrar/sair e mortes de quem está desaparecido não vão para o chat
        ServerMessageEvents.ALLOW_GAME_MESSAGE.register((server, message, overlay) -> !isHiddenMessage(message));
    }

    private static boolean isHiddenMessage(Text message) {
        if (VANISHED.isEmpty()) return false;
        if (!(message.getContent() instanceof TranslatableTextContent t)) return false;
        String key = t.getKey();
        if (!(key.startsWith("chat.type.advancement.") || key.startsWith("multiplayer.player.") || key.startsWith("death."))) return false;
        for (Object arg : t.getArgs()) {
            String s = (arg instanceof Text tx ? tx.getString() : String.valueOf(arg)).toLowerCase(Locale.ROOT);
            for (String name : VANISHED.values()) if (s.contains(name.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    // ---------- desaparecer / aparecer ----------

    public static void vanish(MinecraftServer server, ServerPlayerEntity target) {
        VANISHED.put(target.getUuid(), target.getName().getString());
        save();
        refresh(server, true);
    }

    public static void reveal(MinecraftServer server, ServerPlayerEntity target) {
        VANISHED.remove(target.getUuid());
        save();
        for (ServerPlayerEntity viewer : server.getPlayerManager().getPlayerList()) sendListed(viewer, target, true);
        refresh(server, true);
    }

    // ---------- sincronização com os clientes ----------

    public static void refresh(MinecraftServer server, boolean force) {
        List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
        List<String> entries = new ArrayList<>();
        for (Map.Entry<UUID, String> e : VANISHED.entrySet()) entries.add(e.getKey() + "|" + e.getValue());

        for (ServerPlayerEntity viewer : players) {
            boolean op = isOp(viewer);
            String sig = op + ":" + entries.hashCode();
            if (!force && sig.equals(SIGNATURE.get(viewer.getUuid()))) continue;
            SIGNATURE.put(viewer.getUuid(), sig);

            for (ServerPlayerEntity target : players) {
                if (!VANISHED.containsKey(target.getUuid())) continue;
                sendListed(viewer, target, op || target == viewer);
            }
            if (ServerPlayNetworking.canSend(viewer, VanishPayload.ID)) {
                ServerPlayNetworking.send(viewer, new VanishPayload(op ? List.of() : entries));
            }
        }
    }

    private static void sendListed(ServerPlayerEntity viewer, ServerPlayerEntity target, boolean listed) {
        PlayerListS2CPacket packet = new PlayerListS2CPacket(EnumSet.of(PlayerListS2CPacket.Action.UPDATE_LISTED), List.of(target));
        if (!listed) setListedFalse(packet);
        viewer.networkHandler.sendPacket(packet);
    }

    /**
     * O pacote só sabe criar entradas "listed = true". Aqui troco o campo "listed" (3º componente do record da
     * entrada) por false usando reflexão, sem depender dos nomes internos do jogo.
     */
    private static void setListedFalse(Object packet) {
        try {
            for (Field f : packet.getClass().getDeclaredFields()) {
                if (!List.class.isAssignableFrom(f.getType())) continue;
                f.setAccessible(true);
                List<?> entries = (List<?>) f.get(packet);
                List<Object> out = new ArrayList<>();
                for (Object entry : entries) {
                    RecordComponent[] comps = entry.getClass().getRecordComponents();
                    Object[] values = new Object[comps.length];
                    Class<?>[] types = new Class<?>[comps.length];
                    for (int i = 0; i < comps.length; i++) {
                        Method accessor = comps[i].getAccessor();
                        accessor.setAccessible(true);
                        values[i] = accessor.invoke(entry);
                        types[i] = comps[i].getType();
                    }
                    if (comps.length < 3 || types[2] != boolean.class) throw new IllegalStateException("formato inesperado da entrada");
                    values[2] = Boolean.FALSE;
                    Constructor<?> c = entry.getClass().getDeclaredConstructor(types);
                    c.setAccessible(true);
                    out.add(c.newInstance(values));
                }
                f.set(packet, out);
                return;
            }
        } catch (Exception e) {
            Imdd.LOGGER.warn("Não consegui esconder o jogador do tab: {}", e.toString());
        }
    }

    // ---------- persistência (config/imdd/vanish.json) ----------

    private static Path file() { return Expurgo.dir().resolve("vanish.json"); }

    private static void load() {
        try {
            Path f = file();
            if (!Files.exists(f)) return;
            JsonObject o = GSON.fromJson(Files.readString(f), JsonObject.class);
            VANISHED.clear();
            for (JsonElement e : o.getAsJsonArray("vanished")) {
                JsonObject v = e.getAsJsonObject();
                VANISHED.put(UUID.fromString(v.get("uuid").getAsString()), v.get("name").getAsString());
            }
        } catch (Exception e) {
            Imdd.LOGGER.error("Falha ao ler vanish.json", e);
        }
    }

    private static void save() {
        try {
            JsonObject o = new JsonObject();
            JsonArray arr = new JsonArray();
            for (Map.Entry<UUID, String> e : VANISHED.entrySet()) {
                JsonObject v = new JsonObject();
                v.addProperty("uuid", e.getKey().toString());
                v.addProperty("name", e.getValue());
                arr.add(v);
            }
            o.add("vanished", arr);
            Files.writeString(file(), GSON.toJson(o));
        } catch (Exception e) {
            Imdd.LOGGER.error("Falha ao salvar vanish.json", e);
        }
    }
}
