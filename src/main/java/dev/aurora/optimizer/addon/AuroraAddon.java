package dev.aurora.optimizer.addon;

import dev.aurora.optimizer.AuroraConfig;

/**
 * Punto de entrada para mods separados que quieran aportar funciones a Aurora.
 * Se declara en fabric.mod.json como:
 *
 * "entrypoints": { "aurora": [ "com.ejemplo.MiAddon" ] }
 */
public interface AuroraAddon {
    String id();

    default void onInitialize(AuroraConfig config) {
    }

    default void onTrim() {
    }
}
