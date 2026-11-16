package br.com.irmandade.imdd.client;

import br.com.irmandade.imdd.net.ExpurgoPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;

/** Cliente: marca d'água no F3 (direita, embaixo). */
public class ImddClient implements ClientModInitializer {
    private static final String LINE1 = "Irmandade By LucxMC";
    private static final String LINE2 = "-. .. -. --. ..- . -- / . ... - .- / ... . --. ..- .-. --- /";
    private static volatile long endMs = 0;

    /** true enquanto o Dia do Expurgo está ativo (o servidor envia o tempo restante). */
    public static boolean expurgoActive() {
        return endMs > System.currentTimeMillis();
    }

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(ExpurgoPayload.ID, (payload, context) ->
                endMs = payload.remainingMillis() > 0 ? System.currentTimeMillis() + payload.remainingMillis() : 0);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> endMs = 0);
        TranslucentBlocks.register();
        VanishClient.init();
        HudElementRegistry.addLast(Identifier.of("imdd", "overlay"), ImddClient::render);
    }

    private static void render(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();

        if (mc.getDebugHud().shouldShowDebugHud()) {
            int y = Math.min(Math.max(h / 2 + 25, 372), h - 30);   // direita, logo abaixo das linhas do F3 (que terminam por volta de y=360)
            drawLine(ctx, mc, LINE1, w, y, 0xFFFF55FF);       // Light Purple
            drawLine(ctx, mc, LINE2, w, y + 9, 0xFFFF5555);   // Light Red
        }
    }

    private static void drawLine(DrawContext ctx, MinecraftClient mc, String text, int screenW, int y, int color) {
        int width = mc.textRenderer.getWidth(text);
        int x = screenW - 2 - width;
        ctx.fill(x - 1, y - 1, x + width + 1, y + 8, 0x90505050);
        ctx.drawText(mc.textRenderer, text, x, y, color, false);
    }
}
