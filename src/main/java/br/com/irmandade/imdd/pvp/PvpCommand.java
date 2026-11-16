package br.com.irmandade.imdd.pvp;

import br.com.irmandade.imdd.tab.TabText;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

/** /pvp  |  /pvp on  |  /pvp off  |  /pvp off <tempo>  (ex.: 15m, 1h, 1h30m5s) */
public final class PvpCommand {
    private PvpCommand() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("pvp")
                .requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
                .executes(ctx -> {
                    String msg;
                    if (PvpState.isEnabled()) msg = "&dIMDD &7» &fPvP está &aligado&f.";
                    else if (PvpState.hasTimer()) msg = "&dIMDD &7» &fPvP está &cdesligado &f(volta em &b" + PvpState.human(PvpState.remainingMillis()) + "&f).";
                    else msg = "&dIMDD &7» &fPvP está &cdesligado&f.";
                    ctx.getSource().sendFeedback(() -> TabText.parse(msg), false);
                    return 1;
                })
                .then(CommandManager.literal("on").executes(ctx -> {
                    if (PvpState.isEnabled()) {
                        ctx.getSource().sendError(Text.literal("O PvP já está ligado."));
                        return 0;
                    }
                    PvpState.on(ctx.getSource().getServer());
                    return 1;
                }))
                .then(CommandManager.literal("off")
                        .executes(ctx -> {
                            PvpState.off(ctx.getSource().getServer(), 0);
                            return 1;
                        })
                        .then(CommandManager.argument("tempo", StringArgumentType.word()).executes(ctx -> {
                            long ms = PvpState.parseDuration(StringArgumentType.getString(ctx, "tempo"));
                            if (ms <= 0) {
                                ctx.getSource().sendError(Text.literal("Tempo inválido. Use h, m e s, por exemplo: 15m, 1h ou 1h30m5s."));
                                return 0;
                            }
                            PvpState.off(ctx.getSource().getServer(), ms);
                            return 1;
                        }))));
    }
}
