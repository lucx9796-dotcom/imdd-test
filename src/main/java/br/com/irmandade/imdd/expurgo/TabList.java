package br.com.irmandade.imdd.expurgo;

import br.com.irmandade.imdd.pvp.PvpState;
import br.com.irmandade.imdd.tab.TabConfig;
import br.com.irmandade.imdd.tab.TabText;
import net.minecraft.network.packet.s2c.play.PlayerListHeaderS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Monta o tab a partir de TabConfig (editável pelo comando /tab). */
public final class TabList {
    private static final String LINE = "%linha%";

    private TabList() {}

    public static void updateAll(MinecraftServer server) {
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) update(server, p);
    }

    public static void update(MinecraftServer server, ServerPlayerEntity p) {
        List<String> header = expand(server, p, TabConfig.HEADER);
        List<String> footer = expand(server, p, TabConfig.FOOTER);

        // largura da linha contínua = a linha de texto mais larga do tab (cobre tudo)
        int max = 0;
        for (List<String> list : List.of(header, footer)) {
            for (String l : list) if (!isLine(l)) max = Math.max(max, TabText.width(l));
        }
        int spaces = Math.max(1, (max + 3) / 4); // espaço = 4px; riscado nos espaços = linha reta sem falhas

        p.networkHandler.sendPacket(new PlayerListHeaderS2CPacket(render(header, spaces), render(footer, spaces)));
    }

    private static boolean isLine(String s) { return s.trim().endsWith(LINE); }

    private static List<String> expand(MinecraftServer server, ServerPlayerEntity p, List<String> lines) {
        List<String> out = new ArrayList<>();
        boolean explicitPvp = false;
        for (String l : lines) if (l.trim().equals("%pvp%")) explicitPvp = true;
        for (String raw : lines) {
            String t = raw.trim();
            if (t.equals("%expurgo%")) {
                if (Expurgo.isActive()) {
                    out.add("&#9f0206&lDia do Expurgo");
                    out.add("&7" + format(Expurgo.remainingMillis()));
                }
                // sem uma linha %pvp% própria, o aviso de PvP desligado vem logo depois do Expurgo
                if (!explicitPvp) out.addAll(PvpState.tabLines());
            } else if (t.equals("%pvp%")) {
                out.addAll(PvpState.tabLines());
            } else {
                out.add(replace(server, p, raw));
            }
        }
        return out;
    }

    private static Text render(List<String> lines, int spaces) {
        MutableText out = Text.empty();
        boolean first = true;
        for (String line : lines) {
            if (!first) out.append(Text.literal("\n"));
            first = false;
            if (isLine(line)) {
                String t = line.trim();
                String color = t.substring(0, t.length() - LINE.length());
                if (color.isEmpty()) color = "&b";
                out.append(TabText.parse(color + "&m" + " ".repeat(spaces)));
            } else {
                out.append(TabText.parse(line));
            }
        }
        return out;
    }

    private static String replace(MinecraftServer server, ServerPlayerEntity p, String s) {
        double tps = Math.min(20.0, 1000.0 / Math.max(server.getAverageTickTime(), 1.0f));
        return s.replace("%ping%", String.valueOf(p.networkHandler.getLatency()))
                .replace("%tps%", String.format(Locale.ROOT, "%.1f", tps))
                .replace("%player%", p.getName().getString())
                .replace("%online%", String.valueOf(server.getPlayerManager().getPlayerList().size()))
                .replace("%expurgo_time%", format(Expurgo.remainingMillis()));
    }

    private static String format(long ms) {
        long s = ms / 1000;
        return String.format(Locale.ROOT, "%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60);
    }
}
