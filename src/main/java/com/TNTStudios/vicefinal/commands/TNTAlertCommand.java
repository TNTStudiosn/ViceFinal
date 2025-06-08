package com.TNTStudios.vicefinal.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import toni.immersivemessages.api.ImmersiveMessage;
import toni.immersivemessages.api.SoundEffect;
import toni.immersivemessages.api.TextAnchor;
import toni.immersivemessages.util.ImmersiveColor;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class TNTAlertCommand {

    /**
     * Registra el comando /tntalert, que permite enviar notificaciones personalizadas.
     * Soporta el alias /TNTAlert y requiere nivel de OP 4.
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        // Defino el comportamiento principal del comando /tntalert
        var tntAlertCommand = literal("tntalert")
                // Solo los OPs con nivel 4 o superior pueden usar este comando
                .requires(src -> src.hasPermissionLevel(4))
                // Defino los argumentos que aceptará el comando: (titulo) y (mensaje)
                .then(argument("titulo", StringArgumentType.string())
                        // Para el argumento "mensaje", ahora uso greedyString()
                        // Esto me permite capturar todo el texto restante del comando, incluyendo espacios, sin necesidad de comillas.
                        .then(argument("mensaje", StringArgumentType.greedyString())
                                // Cuando se ejecute el comando con ambos argumentos, llamo a mi método de notificación
                                .executes(TNTAlertCommand::executeCustomNotification)
                        )
                );

        // Registro el comando principal y su alias
        dispatcher.register(tntAlertCommand);
        // El alias /TNTAlert simplemente redirige al comando principal para no repetir lógica
        dispatcher.register(literal("TNTAlert").redirect(dispatcher.getRoot().getChild("tntalert")));
    }


    /**
     * Construye y envía una notificación personalizada a todos los jugadores del servidor.
     * Extrae el título y el mensaje desde los argumentos del comando.
     *
     * @param ctx El contexto del comando, que contiene la fuente y los argumentos.
     * @return Devuelve 1 para indicar que el comando se ejecutó correctamente.
     */
    private static int executeCustomNotification(CommandContext<ServerCommandSource> ctx) {
        // Obtengo el título y el mensaje que el usuario escribió en el comando.
        // El título aún se captura como una sola palabra (con guiones bajos).
        String rawTitle = StringArgumentType.getString(ctx, "titulo");
        // Aquí proceso el título: reemplazo cada guion bajo '_' por un espacio ' '.
        String title = rawTitle.replace('_', ' ');
        String subtitle = StringArgumentType.getString(ctx, "mensaje");

        ServerCommandSource source = ctx.getSource();
        MinecraftServer server = source.getServer();

        // He reutilizado la configuración visual de tu comando /noti Aviso
        // para mantener un estilo consistente en las alertas.
        int color = 0xFF4444; // Rojo de emergencia
        SoundEffect sound = SoundEffect.LOW; // Sonido de alerta


        // Construyo la notificación popup con los argumentos dinámicos
        ImmersiveMessage notification = ImmersiveMessage.popup(10.0f, title, subtitle)
                .sound(sound)                       // Añade efecto de sonido
                .color(color)                       // Establece color del texto
                .style(style -> style.withBold(true).withUnderline(false))  // Hace el texto en negrita
                .background()                       // Añade un fondo
                .backgroundColor(new ImmersiveColor(0, 0, 0, 230)) // Fondo negro con muy poca transparencia (180/255)
                .borderTopColor(new ImmersiveColor(color).mixWith(ImmersiveColor.WHITE, 0.3f)) // Borde superior con variación de color
                .borderBottomColor(new ImmersiveColor(color).mixWith(ImmersiveColor.BLACK, 0.5f)) // Borde inferior con variación de color
                .anchor(TextAnchor.CENTER_CENTER)   // Centrado en la pantalla
                .y(40f)                             // Posición más arriba, encima del crosshair
                .wrap(250)                          // Ajusta el ancho del texto
                .slideUp(0.5f)                      // Efecto de deslizamiento hacia arriba
                .slideOutDown(0.5f)                 // Efecto de deslizamiento hacia abajo al final
                .fadeIn(0.5f)                       // Efecto de aparición
                .fadeOut(0.5f)                      // Efecto de desvanecimiento
                .subtext(0.5f, subtitle, 10f, (subtext) ->  // Subí la posición vertical de 20f a 10f
                        subtext
                                .size(0.8f)              // Tamaño ligeramente menor
                                .wrap(250)               // Mismo ancho de línea
                                .anchor(TextAnchor.CENTER_CENTER) // Centrado
                                .slideUp(0.5f)           // Añadido efecto de deslizamiento hacia arriba para el subtítulo
                                .slideOutDown(0.5f)      // Añadido efecto de deslizamiento hacia abajo al final para el subtítulo
                                .fadeIn(0.5f)            // Añadido efecto de aparición
                                .fadeOut(0.5f)           // Añadido efecto de desvanecimiento
                )
                .typewriter(1.2f, true);            // Efecto de escritura de máquina

        // Envía la notificación a todos los jugadores del servidor
        notification.sendServerToAll(server);

        return 1; // Éxito
    }
}