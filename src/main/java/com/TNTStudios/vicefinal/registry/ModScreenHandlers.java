package com.TNTStudios.vicefinal.registry;

import com.TNTStudios.vicefinal.screen.NucleoScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType; // <--- [AÑADIR] Importar la clase correcta de Fabric API.
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import com.TNTStudios.vicefinal.screen.BossChallengeScreenHandler;
import net.minecraft.resource.featuretoggle.FeatureFlags;

public class ModScreenHandlers {


    public static final ScreenHandlerType<NucleoScreenHandler> NUCLEO_SCREEN_HANDLER =
            new ExtendedScreenHandlerType<>(NucleoScreenHandler::new);

    // AÑADO MI NUEVO SCREEN HANDLER
    // No necesita ser 'Extended' porque no le paso datos extra al abrirlo.
    // El estado se gestiona después con PropertyDelegate.
    public static final ScreenHandlerType<BossChallengeScreenHandler> BOSS_CHALLENGE_SCREEN_HANDLER =
            new ScreenHandlerType<>(BossChallengeScreenHandler::new, FeatureFlags.VANILLA_FEATURES);


    public static void register() {
        Registry.register(Registries.SCREEN_HANDLER, new Identifier("vicefinal", "nucleo"), NUCLEO_SCREEN_HANDLER);
        // LO REGISTRO CON SU IDENTIFICADOR
        Registry.register(Registries.SCREEN_HANDLER, new Identifier("vicefinal", "boss_challenge"), BOSS_CHALLENGE_SCREEN_HANDLER);
    }
}