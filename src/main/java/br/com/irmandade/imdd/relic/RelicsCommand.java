package br.com.irmandade.imdd.relic;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.UUID;

public final class RelicsCommand {
    private RelicsCommand() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("relics")
                // Só operadores (nível 2). Para liberar a todos, remova a linha abaixo.
                .requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
                .executes(ctx -> {
                    RelicsMenu.open(ctx.getSource().getPlayerOrThrow());
                    return 1;
                })
                .then(CommandManager.literal("entregues").executes(ctx -> {
                    ServerCommandSource src = ctx.getSource();
                    for (String id : RelicRegistry.ids()) {
                        String owner = RelicDelivery.delivered().get(id);
                        String status;
                        if (owner != null) {
                            ServerPlayerEntity online = src.getServer().getPlayerManager().getPlayer(UUID.fromString(owner));
                            status = "entregue a " + (online != null ? online.getName().getString() : owner);
                        } else if (RelicDelivery.EXCLUDED.contains(id)) {
                            status = "conquista especial (não é entregue)";
                        } else {
                            status = "livre";
                        }
                        String line = id + ": " + status;
                        src.sendFeedback(() -> Text.literal(line), false);
                    }
                    return 1;
                }))
                .then(CommandManager.literal("liberar")
                        .then(CommandManager.argument("id", StringArgumentType.word()).executes(ctx -> {
                            String id = StringArgumentType.getString(ctx, "id");
                            if (RelicDelivery.release(id)) {
                                ctx.getSource().sendFeedback(() -> Text.literal("Relíquia '" + id + "' liberada."), true);
                                return 1;
                            }
                            ctx.getSource().sendError(Text.literal("A relíquia '" + id + "' não estava registrada como entregue."));
                            return 0;
                        }))));
    }
}
