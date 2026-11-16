package br.com.irmandade.imdd.expurgo;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Command;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

/** /expurgo parar  (ou /expurgo encerrar): termina o Dia do Expurgo na hora. */
public final class ExpurgoCommand {
    private ExpurgoCommand() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        Command<ServerCommandSource> stop = ctx -> {
            ServerCommandSource src = ctx.getSource();
            if (!Expurgo.isActive()) {
                src.sendError(Text.literal("O Dia do Expurgo não está ativo."));
                return 0;
            }
            Expurgo.end(src.getServer());
            src.sendFeedback(() -> Text.literal("Dia do Expurgo encerrado."), true);
            return 1;
        };
        dispatcher.register(CommandManager.literal("expurgo")
                .requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
                .then(CommandManager.literal("parar").executes(stop))
                .then(CommandManager.literal("encerrar").executes(stop)));
    }
}
