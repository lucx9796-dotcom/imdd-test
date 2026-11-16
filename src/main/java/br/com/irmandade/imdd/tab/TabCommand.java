package br.com.irmandade.imdd.tab;

import br.com.irmandade.imdd.expurgo.TabList;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.List;

/**
 * /tab list | reset | reload
 * /tab header|footer add <texto> | set <linha> <texto> | insert <linha> <texto> | remove <linha> | clear
 */
public final class TabCommand {
    private TabCommand() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("tab")
                .requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
                .then(section("header", TabConfig.HEADER))
                .then(section("footer", TabConfig.FOOTER))
                .then(CommandManager.literal("list").executes(ctx -> {
                    list(ctx.getSource(), "header", TabConfig.HEADER);
                    list(ctx.getSource(), "footer", TabConfig.FOOTER);
                    return 1;
                }))
                .then(CommandManager.literal("reset").executes(ctx -> {
                    TabConfig.defaults();
                    return done(ctx.getSource(), "Tab restaurado para o padrão.");
                }))
                .then(CommandManager.literal("reload").executes(ctx -> {
                    TabConfig.load();
                    return done(ctx.getSource(), "Tab recarregado do arquivo tab.json.");
                })));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> section(String name, List<String> lines) {
        return CommandManager.literal(name)
                .then(CommandManager.literal("add")
                        .then(CommandManager.argument("texto", StringArgumentType.greedyString()).executes(ctx -> {
                            lines.add(StringArgumentType.getString(ctx, "texto"));
                            return done(ctx.getSource(), "Linha adicionada ao " + name + ".");
                        })))
                .then(CommandManager.literal("set")
                        .then(CommandManager.argument("linha", IntegerArgumentType.integer(1))
                                .then(CommandManager.argument("texto", StringArgumentType.greedyString()).executes(ctx -> {
                                    int n = IntegerArgumentType.getInteger(ctx, "linha");
                                    if (n > lines.size()) return error(ctx.getSource(), name, lines);
                                    lines.set(n - 1, StringArgumentType.getString(ctx, "texto"));
                                    return done(ctx.getSource(), "Linha " + n + " do " + name + " alterada.");
                                }))))
                .then(CommandManager.literal("insert")
                        .then(CommandManager.argument("linha", IntegerArgumentType.integer(1))
                                .then(CommandManager.argument("texto", StringArgumentType.greedyString()).executes(ctx -> {
                                    int n = IntegerArgumentType.getInteger(ctx, "linha");
                                    if (n > lines.size() + 1) return error(ctx.getSource(), name, lines);
                                    lines.add(n - 1, StringArgumentType.getString(ctx, "texto"));
                                    return done(ctx.getSource(), "Linha inserida na posição " + n + " do " + name + ".");
                                }))))
                .then(CommandManager.literal("remove")
                        .then(CommandManager.argument("linha", IntegerArgumentType.integer(1)).executes(ctx -> {
                            int n = IntegerArgumentType.getInteger(ctx, "linha");
                            if (n > lines.size()) return error(ctx.getSource(), name, lines);
                            lines.remove(n - 1);
                            return done(ctx.getSource(), "Linha " + n + " removida do " + name + ".");
                        })))
                .then(CommandManager.literal("clear").executes(ctx -> {
                    lines.clear();
                    return done(ctx.getSource(), name + " limpo.");
                }));
    }

    private static int done(ServerCommandSource src, String msg) {
        TabConfig.save();
        TabList.updateAll(src.getServer());
        src.sendFeedback(() -> Text.literal(msg), false);
        return 1;
    }

    private static int error(ServerCommandSource src, String name, List<String> lines) {
        src.sendError(Text.literal("Linha inválida. O " + name + " tem " + lines.size() + " linha(s)."));
        return 0;
    }

    private static void list(ServerCommandSource src, String name, List<String> lines) {
        src.sendFeedback(() -> Text.literal("— " + name + " (" + lines.size() + ") —"), false);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int n = i + 1;
            src.sendFeedback(() -> Text.literal(n + ": " + line), false);
        }
    }
}
