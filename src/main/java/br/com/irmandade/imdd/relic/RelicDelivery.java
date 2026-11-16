package br.com.irmandade.imdd.relic;

import br.com.irmandade.imdd.Imdd;
import br.com.irmandade.imdd.expurgo.Expurgo;
import br.com.irmandade.imdd.tab.TabText;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;

/**
 * Entrega de relíquias:
 *  - jogador novo no mapa recebe 1 relíquia aleatória, ainda não entregue, no baú do fim;
 *  - cada relíquia só é entregue uma vez (registro em config/imdd/relics_delivery.json);
 *  - Dragão, machado do Bizonho, Stomper e Warden NÃO entram no sorteio (têm outra forma de conquista);
 *  - Dragão: jogar o ovo de dragão no void do End dá a relíquia ao jogador que jogou.
 */
public final class RelicDelivery {
    /** Relíquias que não são entregues a jogadores novos. */
    public static final Set<String> EXCLUDED = Set.of("dragao", "bizonho_machado", "stomper", "warden");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, String> DELIVERED = new LinkedHashMap<>(); // id da relíquia -> uuid do dono
    private static final Set<String> KNOWN = new HashSet<>();                    // jogadores já avaliados
    private static final Map<UUID, UUID> EGG_OWNER = new HashMap<>();            // ovo (item) -> quem jogou
    private static final Random RANDOM = new Random();
    private static final Predicate<ServerCommandSource> OP = CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK);

    private RelicDelivery() {}

    public static void init() {
        ServerLifecycleEvents.SERVER_STARTED.register(s -> load());
        ServerLifecycleEvents.SERVER_STOPPING.register(s -> save());
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> onJoin(handler.getPlayer()));
        ServerEntityEvents.ENTITY_LOAD.register(RelicDelivery::onEntityLoad);
        ServerTickEvents.END_SERVER_TICK.register(RelicDelivery::tick);
    }

    // ---------- jogador novo ----------

    /** Jogadores com OP não recebem relíquia no baú do fim. */
    private static boolean isOp(ServerPlayerEntity p) {
        try { return OP.test(p.getCommandSource()); } catch (Exception e) { return false; }
    }

    private static void onJoin(ServerPlayerEntity p) {
        if (isOp(p)) return;
        String uuid = p.getUuid().toString();
        if (KNOWN.contains(uuid)) return;
        KNOWN.add(uuid);

        // Quem já tinha tempo de jogo antes do mod não é "novo no mapa"
        int played = p.getStatHandler().getStat(Stats.CUSTOM, Stats.PLAY_TIME);
        if (played > 0) { save(); return; }

        String id = pickFree();
        if (id == null) {
            Imdd.LOGGER.info("Sem relíquias livres para entregar a {}", p.getName().getString());
            save();
            return;
        }
        ItemStack relic = RelicRegistry.copyOf(id);
        ItemStack rest = p.getEnderChestInventory().addStack(relic);
        if (!rest.isEmpty()) {            // baú cheio (não deveria acontecer com jogador novo)
            p.getInventory().insertStack(rest);
            if (!rest.isEmpty()) p.dropItem(rest, false);
        }
        DELIVERED.put(id, uuid);
        save();
        p.sendMessage(TabText.parse("&dIMDD &7» &fVocê recebeu uma relíquia no seu baú do fim!"), false);
    }

    private static String pickFree() {
        List<String> free = new ArrayList<>();
        for (String id : RelicRegistry.ids()) {
            if (EXCLUDED.contains(id) || DELIVERED.containsKey(id)) continue;
            if (RelicRegistry.copyOf(id).isEmpty()) continue;
            free.add(id);
        }
        return free.isEmpty() ? null : free.get(RANDOM.nextInt(free.size()));
    }

    // ---------- Dragão: ovo jogado no void do End ----------

    private static void onEntityLoad(Entity entity, ServerWorld world) {
        if (world.getRegistryKey() != World.END) return;
        if (!(entity instanceof ItemEntity item) || !item.getStack().isOf(Items.DRAGON_EGG)) return;
        if (EGG_OWNER.containsKey(item.getUuid())) return;
        PlayerEntity near = world.getClosestPlayer(entity, 6.0);
        EGG_OWNER.put(item.getUuid(), near == null ? null : near.getUuid());
    }

    private static void tick(MinecraftServer server) {
        if (EGG_OWNER.isEmpty() || server.getTicks() % 5 != 0) return;
        ServerWorld end = server.getWorld(World.END);
        if (end == null) return;
        Iterator<Map.Entry<UUID, UUID>> it = EGG_OWNER.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, UUID> e = it.next();
            Entity ent = end.getEntity(e.getKey());
            if (!(ent instanceof ItemEntity egg) || egg.isRemoved()) { it.remove(); continue; }
            if (egg.getY() >= end.getBottomY() - 8) continue;    // ainda não caiu no void

            ServerPlayerEntity p = e.getValue() == null ? null : server.getPlayerManager().getPlayer(e.getValue());
            if (p == null) {
                PlayerEntity near = end.getClosestPlayer(egg.getX(), 0, egg.getZ(), 256, false);
                if (near instanceof ServerPlayerEntity sp) p = sp;
            }
            it.remove();
            egg.discard();
            if (p != null) grantDragon(p);
        }
    }

    private static void grantDragon(ServerPlayerEntity p) {
        if (DELIVERED.containsKey("dragao")) {
            p.sendMessage(TabText.parse("&dIMDD &7» &cA Relíquia do Dragão já foi conquistada."), false);
            return;
        }
        ItemStack relic = RelicRegistry.copyOf("dragao");
        if (relic.isEmpty()) return;
        p.getInventory().insertStack(relic);
        if (!relic.isEmpty()) p.dropItem(relic, false);
        DELIVERED.put("dragao", p.getUuid().toString());
        save();
        p.sendMessage(TabText.parse("&dIMDD &7» &fVocê conquistou uma relíquia!"), false);
    }

    // ---------- administração (/relics entregues, /relics liberar <id>) ----------

    public static Map<String, String> delivered() { return DELIVERED; }

    /** Libera uma relíquia (ex.: perdida no void) para poder ser entregue/conquistada de novo. */
    public static boolean release(String id) {
        boolean had = DELIVERED.remove(id) != null;
        if (had) save();
        return had;
    }

    // ---------- persistência ----------

    private static Path file() { return Expurgo.dir().resolve("relics_delivery.json"); }

    private static void load() {
        try {
            Path f = file();
            if (!Files.exists(f)) return;
            JsonObject o = GSON.fromJson(Files.readString(f), JsonObject.class);
            DELIVERED.clear();
            KNOWN.clear();
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("delivered").entrySet()) DELIVERED.put(e.getKey(), e.getValue().getAsString());
            for (JsonElement e : o.getAsJsonArray("known")) KNOWN.add(e.getAsString());
        } catch (Exception e) {
            Imdd.LOGGER.error("Falha ao ler relics_delivery.json", e);
        }
    }

    private static void save() {
        try {
            JsonObject o = new JsonObject(), d = new JsonObject();
            JsonArray k = new JsonArray();
            DELIVERED.forEach(d::addProperty);
            KNOWN.forEach(k::add);
            o.add("delivered", d);
            o.add("known", k);
            Files.writeString(file(), GSON.toJson(o));
        } catch (Exception e) {
            Imdd.LOGGER.error("Falha ao salvar relics_delivery.json", e);
        }
    }
}
