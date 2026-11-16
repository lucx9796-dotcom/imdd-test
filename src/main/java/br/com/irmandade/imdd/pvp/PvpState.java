package br.com.irmandade.imdd.pvp;

import br.com.irmandade.imdd.Imdd;
import br.com.irmandade.imdd.expurgo.Expurgo;
import br.com.irmandade.imdd.expurgo.TabList;
import br.com.irmandade.imdd.tab.TabText;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * PvP global do servidor: /pvp on | off | off <tempo>.
 * O tempo usa h, m e s (15m, 1h, 1h30m5s) e é em tempo real; o estado fica salvo em config/imdd/pvp.json.
 */
public final class PvpState {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static boolean enabled = true;
    private static long offUntil = 0; // 0 = desligado sem prazo

    private PvpState() {}

    public static boolean isEnabled() { return enabled; }

    public static boolean hasTimer() { return !enabled && offUntil > 0; }

    public static long remainingMillis() { return Math.max(0, offUntil - System.currentTimeMillis()); }

    public static void init() {
        ServerLifecycleEvents.SERVER_STARTED.register(s -> load());
        ServerLifecycleEvents.SERVER_STOPPING.register(s -> save());
        ServerTickEvents.END_SERVER_TICK.register(s -> {
            if (!enabled && offUntil > 0 && System.currentTimeMillis() >= offUntil) on(s);
        });
        // PvP desligado: ninguém causa dano em jogador (inclui flechas, tridente etc., pois o atacante é o dono)
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!enabled && entity instanceof ServerPlayerEntity && source.getAttacker() instanceof PlayerEntity attacker && attacker != entity) {
                return false;
            }
            return true;
        });
    }

    // ---------- ligar / desligar ----------

    public static void off(MinecraftServer server, long millis) {
        enabled = false;
        offUntil = millis > 0 ? System.currentTimeMillis() + millis : 0;
        save();
        String msg = "&dIMDD &7» &fPvP &cdesligado" + (millis > 0 ? " &fpor &b" + human(millis) : "") + "&f.";
        server.getPlayerManager().broadcast(TabText.parse(msg), false);
        TabList.updateAll(server);
    }

    public static void on(MinecraftServer server) {
        enabled = true;
        offUntil = 0;
        save();
        server.getPlayerManager().broadcast(TabText.parse("&dIMDD &7» &fPvP &aligado&f."), false);
        TabList.updateAll(server);
    }

    // ---------- tab ----------

    /** Linhas do tab quando o PvP está desligado (vazio quando ligado). Cor ciano, como o Expurgo é vermelho. */
    public static List<String> tabLines() {
        List<String> lines = new ArrayList<>();
        if (enabled) return lines;
        lines.add("&b&lPVP Desligado");
        if (offUntil > 0) lines.add("&7" + clock(remainingMillis()));
        return lines;
    }

    // ---------- tempo ----------

    /** "15m", "1h", "1h30m5s" -> milissegundos; -1 se inválido. */
    public static long parseDuration(String in) {
        long total = 0, num = -1;
        for (char raw : in.trim().toLowerCase(Locale.ROOT).toCharArray()) {
            if (raw >= '0' && raw <= '9') {
                num = (num < 0 ? 0 : num) * 10 + (raw - '0');
                if (num > 10_000_000L) return -1;
            } else if (raw == 'h' || raw == 'm' || raw == 's') {
                if (num < 0) return -1;
                total += num * (raw == 'h' ? 3600 : raw == 'm' ? 60 : 1);
                num = -1;
            } else {
                return -1;
            }
        }
        if (num >= 0 || total <= 0) return -1;
        return total * 1000L;
    }

    public static String human(long ms) {
        long s = ms / 1000;
        StringBuilder b = new StringBuilder();
        if (s >= 3600) b.append(s / 3600).append("h");
        if ((s % 3600) >= 60) b.append((s % 3600) / 60).append("m");
        if (s % 60 > 0 || b.length() == 0) b.append(s % 60).append("s");
        return b.toString();
    }

    private static String clock(long ms) {
        long s = ms / 1000;
        return String.format(Locale.ROOT, "%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60);
    }

    // ---------- persistência ----------

    private static Path file() { return Expurgo.dir().resolve("pvp.json"); }

    private static void load() {
        try {
            Path f = file();
            if (!Files.exists(f)) return;
            JsonObject o = GSON.fromJson(Files.readString(f), JsonObject.class);
            enabled = o.get("enabled").getAsBoolean();
            offUntil = o.get("offUntil").getAsLong();
        } catch (Exception e) {
            Imdd.LOGGER.error("Falha ao ler pvp.json", e);
        }
    }

    private static void save() {
        try {
            JsonObject o = new JsonObject();
            o.addProperty("enabled", enabled);
            o.addProperty("offUntil", offUntil);
            Files.writeString(file(), GSON.toJson(o));
        } catch (Exception e) {
            Imdd.LOGGER.error("Falha ao salvar pvp.json", e);
        }
    }
}
