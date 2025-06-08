package com.TNTStudios.vicefinal.registry;

import com.TNTStudios.vicefinal.screen.NucleoScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType; // <--- [AÑADIR] Importar la clase correcta de Fabric API.
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public class ModScreenHandlers {

    // Defino el tipo de ScreenHandler para el núcleo.
    // Como mi ScreenHandler necesita recibir datos extra en el cliente (la posición del BlockEntity),
    // utilizo ExtendedScreenHandlerType de Fabric API. Este espera un constructor que acepte un PacketByteBuf.
    public static final ScreenHandlerType<NucleoScreenHandler> NUCLEO_SCREEN_HANDLER =
            new ExtendedScreenHandlerType<>(NucleoScreenHandler::new); // <--- [MODIFICAR] Usar ExtendedScreenHandlerType.

    public static void register() {
        // El registro se mantiene igual, solo que ahora el objeto que registramos es del tipo correcto.
        Registry.register(Registries.SCREEN_HANDLER, new Identifier("vicefinal", "nucleo"), NUCLEO_SCREEN_HANDLER);
    }
}