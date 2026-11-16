package br.com.irmandade.imdd.vanish;

import br.com.irmandade.imdd.tab.TabText;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.UUID;

/** /desaparecer [jogador]  |  /aparecer <jogador>  (só operadores) */
public final class VanishCommand {
    private VanishCommand() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("desaparecer")
                .requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
                .executes(ctx -> {
                    ServerCommandSource src = ctx.getSource();
                    if (VanishState.vanished().isEmpty()) {
                        src.sendFeedback(() -> TabText.parse("&dIMDD &7» &fNinguém está desaparecido."), false);
                        return 1;
                    }
                    src.sendFeedback(() -> TabText.parse("&dIMDD &7» &fDesaparecidos:"), false);
                    for (Map.Entry<UUID, String> e : VanishState.vanished().entrySet()) {
                        String name = e.getValue();
                        src.sendFeedback(() -> TabText.parse("&7- &b" + name), false);
                    }
                    return 1;
                })
                .then(CommandManager.argument("jogador", EntityArgumentType.player()).executes(ctx -> {
                    ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "jogador");
                    VanishState.vanish(ctx.getSource().getServer(), target);
                    String name = target.getName().getString();
                    ctx.getSource().sendFeedback(() -> TabText.parse("&dIMDD &7» &b" + name + " &fagora está &bdesaparecido&f."), false);
                    return 1;
                })));

        dispatcher.register(CommandManager.literal("aparecer")
                .requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
                .then(CommandManager.argument("jogador", EntityArgumentType.player()).executes(ctx -> {
                    ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "jogador");
                    VanishState.reveal(ctx.getSource().getServer(), target);
                    String name = target.getName().getString();
                    ctx.getSource().sendFeedback(() -> TabText.parse("&dIMDD &7» &b" + name + " &fvoltou a &aaparecer&f."), false);
                    return 1;
                })));
    }
}
