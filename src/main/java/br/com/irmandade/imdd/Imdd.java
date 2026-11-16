package br.com.irmandade.imdd;

import br.com.irmandade.imdd.expurgo.Expurgo;
import br.com.irmandade.imdd.expurgo.ExpurgoCommand;
import br.com.irmandade.imdd.net.ExpurgoPayload;
import br.com.irmandade.imdd.relic.ModBlocks;
import br.com.irmandade.imdd.relic.RelicAbilities;
import br.com.irmandade.imdd.relic.RelicEffects;
import br.com.irmandade.imdd.relic.RelicRegistry;
import br.com.irmandade.imdd.relic.RelicsCommand;
import br.com.irmandade.imdd.relic.SpyEffect;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Imdd implements ModInitializer {
    public static final String MOD_ID = "imdd";
    public static final Logger LOGGER = LoggerFactory.getLogger("IMDD");

    @Override
    public void onInitialize() {
        ModBlocks.register();
        br.com.irmandade.imdd.relic.ModRecipes.register();
        SpyEffect.register();
        PayloadTypeRegistry.playS2C().register(ExpurgoPayload.ID, ExpurgoPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(br.com.irmandade.imdd.vanish.VanishPayload.ID, br.com.irmandade.imdd.vanish.VanishPayload.CODEC);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) -> { RelicsCommand.register(dispatcher); ExpurgoCommand.register(dispatcher); br.com.irmandade.imdd.tab.TabCommand.register(dispatcher); br.com.irmandade.imdd.pvp.PvpCommand.register(dispatcher); br.com.irmandade.imdd.vanish.VanishCommand.register(dispatcher); });
        ServerLifecycleEvents.SERVER_STARTED.register(server -> RelicRegistry.load(server.getRegistryManager()));
        RelicEffects.init();
        RelicAbilities.init();
        Expurgo.init();
        br.com.irmandade.imdd.relic.RelicDelivery.init();
        br.com.irmandade.imdd.pvp.PvpState.init();
        br.com.irmandade.imdd.vanish.VanishState.init();
        LOGGER.info("Irmandade (IMDD) carregado.");
    }
}
