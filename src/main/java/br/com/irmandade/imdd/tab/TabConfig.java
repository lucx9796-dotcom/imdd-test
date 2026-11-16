package br.com.irmandade.imdd.tab;

import br.com.irmandade.imdd.Imdd;
import br.com.irmandade.imdd.expurgo.Expurgo;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Linhas do tab (config/imdd/tab.json). Códigos: &cores, &#RRGGBB, &l negrito...
 * Placeholders: %ping% %tps% %player% %online% %expurgo_time%
 * A linha %linha% desenha uma linha reta contínua (ciano) cobrindo a largura do tab; use "&c%linha%" para outra cor.
 * A linha %pvp% mostra "PVP Desligado" (ciano) + contagem quando o /pvp off está ativo; sem ela, aparece logo após %expurgo%.
 * A linha especial %expurgo% vira "Dia do Expurgo" + contagem (só aparece quando o Expurgo está ativo).
 * O "ポ" do logo é o caractere do resource pack do servidor.
 */
public final class TabConfig {
    public static final List<String> HEADER = new ArrayList<>();
    public static final List<String> FOOTER = new ArrayList<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    static { defaults(); }

    private TabConfig() {}

    public static void defaults() {
        HEADER.clear();
        FOOTER.clear();
        HEADER.add("&f");
        HEADER.add("&f");
        HEADER.add("&f");
        HEADER.add("&f");
        HEADER.add("&fポ");
        HEADER.add("&f");
        HEADER.add("%expurgo%");
        HEADER.add("%linha%");
        FOOTER.add("%linha%");
        FOOTER.add("&7Ping: &a%ping%ms &f– &7TPS: &a%tps%");
        FOOTER.add("&7Feito por: &bLucxMC_ &8| &7Cupom: &b&lIRMANDADE");
        FOOTER.add("&7Servidor: &bEnxadaHost");
        FOOTER.add("");
        FOOTER.add("&7irmandade.gg");
    }

    private static Path file() { return Expurgo.dir().resolve("tab.json"); }

    public static void load() {
        try {
            Path f = file();
            if (!Files.exists(f)) { save(); return; }
            JsonObject o = GSON.fromJson(Files.readString(f), JsonObject.class);
            HEADER.clear();
            FOOTER.clear();
            for (var e : o.getAsJsonArray("header")) HEADER.add(migrate(e.getAsString()));
            for (var e : o.getAsJsonArray("footer")) FOOTER.add(migrate(e.getAsString()));
        } catch (Exception e) {
            Imdd.LOGGER.error("Falha ao ler tab.json (usando padrão)", e);
            defaults();
        }
    }

    /** Linhas antigas feitas de "━" viram a linha contínua %linha%. */
    private static String migrate(String s) {
        return s.contains("━━") ? "%linha%" : s;
    }

    public static void save() {
        try {
            JsonObject o = new JsonObject();
            JsonArray h = new JsonArray(), f = new JsonArray();
            HEADER.forEach(h::add);
            FOOTER.forEach(f::add);
            o.add("header", h);
            o.add("footer", f);
            Files.writeString(file(), GSON.toJson(o));
        } catch (Exception e) {
            Imdd.LOGGER.error("Falha ao salvar tab.json", e);
        }
    }
}
