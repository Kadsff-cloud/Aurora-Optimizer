package dev.aurora.optimizer.client;

import dev.aurora.optimizer.Aurora;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prueba viva de que los mixins se aplicaron y se ejecutan. Se registra una
 * sola vez por hook para no inundar el log; /aurora status muestra el resumen.
 *
 * El resumen visible se localiza; el resumen del log conserva los nombres
 * internos porque los scripts de prueba buscan "Mixin activo: nivel".
 */
public final class HookProbe {
    private static final Set<String> SEEN = ConcurrentHashMap.newKeySet();

    private HookProbe() {
    }

    public static void mark(String hook) {
        if (SEEN.add(hook)) {
            Aurora.LOGGER.info("Mixin activo: {}", hook);
        }
    }

    /** Resumen localizado para el chat. */
    public static Component summary() {
        if (SEEN.isEmpty()) {
            return Component.translatable("aurora.hooks.none");
        }
        List<String> hooks = SEEN.stream().sorted().toList();
        MutableComponent result = Component.empty();
        for (int i = 0; i < hooks.size(); i++) {
            if (i > 0) {
                result.append(Component.literal(", "));
            }
            result.append(label(hooks.get(i)));
        }
        return result;
    }

    /** Resumen estable para logs y scripts. */
    public static String summaryString() {
        return SEEN.isEmpty() ? "ninguno" : String.join(", ", SEEN);
    }

    private static Component label(String hook) {
        return switch (hook) {
            case "entidades" -> Component.translatable("aurora.hook.entities");
            case "particulas" -> Component.translatable("aurora.hook.particles");
            case "nivel" -> Component.translatable("aurora.hook.level");
            case "render" -> Component.translatable("aurora.hook.render");
            default -> Component.literal(hook);
        };
    }
}
