package br.com.irmandade.imdd.expurgo;

import br.com.irmandade.imdd.Imdd;
import br.com.irmandade.imdd.net.ExpurgoPayload;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.nio.file.Files;
import java.nio.file.Path;

/** Dia do Expurgo: queimar uma nether star ativa 24h (cada nova estrela soma +24h). */
public final class Expurgo {
    private static final long DAY = 24L * 60 * 60 * 1000;
    private static final Gson GSON = new Gson();
    private static long endMillis = 0;
    private static MinecraftServer server;

    private Expurgo() {}

    public static boolean isActive() { return endMillis > System.currentTimeMillis(); }

    public static long remainingMillis() { return Math.max(0, endMillis - System.currentTimeMillis()); }

    public static Path dir() {
        Path d = FabricLoader.getInstance().getConfigDir().resolve("imdd");
        try { Files.createDirectories(d); } catch (Exception ignored) {}
        return d;
    }

    public static void init() {
        ServerLifecycleEvents.SERVER_STARTED.register(s -> { server = s; load(); ExpurgoNpcs.load(); br.com.irmandade.imdd.tab.TabConfig.load(); });
        ServerLifecycleEvents.SERVER_STOPPING.register(s -> save());
        ServerTickEvents.END_SERVER_TICK.register(Expurgo::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, s) -> {
            ServerPlayerEntity p = handler.getPlayer();
            ExpurgoNpcs.onJoin(p, s);
            sync(p);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, s) -> ExpurgoNpcs.onDisconnect(handler.getPlayer(), s));
        ServerLivingEntityEvents.AFTER_DEATH.register(ExpurgoNpcs::onDeath);
        ServerEntityEvents.ENTITY_LOAD.register(ExpurgoNpcs::onEntityLoad);
    }

    // ---------- ativação ----------

    public static void onNetherStarBurned(ServerWorld world, Vec3d pos, int count) {
        MinecraftServer s = world.getServer();
        long now = System.currentTimeMillis();
        boolean was = isActive();
        endMillis = (was ? endMillis : now) + DAY * Math.max(1, count);
        save();

        if (!was) run(s, "execute in minecraft:overworld run time set 18000"); // meia-noite de lua cheia
        showTitle(s, was ? "O Tempo foi Extendido!" : "24 Horas Sem Regras ativado!");

        LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(world, SpawnReason.TRIGGERED);
        if (bolt != null) {
            bolt.refreshPositionAfterTeleport(pos.x, pos.y, pos.z);
            bolt.setCosmetic(true);
            world.spawnEntity(bolt);
        }
        for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList()) sync(p);
    }

    public static void end(MinecraftServer s) {
        endMillis = 0;
        save();
        ExpurgoNpcs.discardAll(s);
        run(s, "execute in minecraft:overworld run time set 0");
        s.getPlayerManager().broadcast(Text.literal("☠ O Dia do Expurgo terminou.").styled(st -> st.withColor(0xb30000)), false);
        for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList()) sync(p);
    }

    private static void tick(MinecraftServer s) {
        int t = s.getTicks();
        if (endMillis != 0 && !isActive()) end(s);
        if (isActive() && t % 100 == 0) run(s, "execute in minecraft:overworld run time set 18000"); // mantém a noite de lua cheia
        if (t % 20 == 0) for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList()) TabList.update(s, p);
    }

    // ---------- mensagens ----------

    private static void showTitle(MinecraftServer s, String subtitle) {
        MutableText title = Text.empty()
                .append(Text.literal("☠").styled(st -> st.withColor(0xb30000)))
                .append(Text.literal(" "))
                .append(Text.literal("Dia do Expurgo").styled(st -> st.withColor(0xb30000).withBold(true)))
                .append(Text.literal(" "))
                .append(Text.literal("☠").styled(st -> st.withColor(0xb30000)));
        Text sub = Text.literal(subtitle).styled(st -> st.withColor(0x757575));
        for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList()) {
            p.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 70, 20));
            p.networkHandler.sendPacket(new SubtitleS2CPacket(sub));
            p.networkHandler.sendPacket(new TitleS2CPacket(title));
        }
    }

    public static Text enderChestMessage() {
        return Text.empty()
                .append(Text.literal("IMDD").formatted(Formatting.LIGHT_PURPLE))
                .append(Text.literal(" "))
                .append(Text.literal("»").styled(st -> st.withColor(0xa096b2)))
                .append(Text.literal(" "))
                .append(Text.literal("Você não pode abrir o baú do fim durante o").styled(st -> st.withColor(0xdc5663)))
                .append(Text.literal(" "))
                .append(Text.literal("Dia do Expurgo").styled(st -> st.withColor(0x9f0206).withBold(true)));
    }

    // ---------- util ----------

    public static void sync(ServerPlayerEntity p) {
        if (ServerPlayNetworking.canSend(p, ExpurgoPayload.ID)) ServerPlayNetworking.send(p, new ExpurgoPayload(remainingMillis()));
    }

    static void run(MinecraftServer s, String command) {
        try {
            s.getCommandManager().getDispatcher().execute(command, s.getCommandSource().withSilent());
        } catch (Exception e) {
            Imdd.LOGGER.error("Falha ao executar comando interno '{}': {}", command, e.getMessage());
        }
    }

    private static void load() {
        try {
            Path f = dir().resolve("expurgo.json");
            if (Files.exists(f)) endMillis = GSON.fromJson(Files.readString(f), JsonObject.class).get("endMillis").getAsLong();
        } catch (Exception e) { Imdd.LOGGER.error("Falha ao ler expurgo.json", e); }
    }

    private static void save() {
        try {
            JsonObject o = new JsonObject();
            o.addProperty("endMillis", endMillis);
            Files.writeString(dir().resolve("expurgo.json"), GSON.toJson(o));
        } catch (Exception e) { Imdd.LOGGER.error("Falha ao salvar expurgo.json", e); }
    }
}
