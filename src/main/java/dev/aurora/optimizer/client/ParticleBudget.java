package dev.aurora.optimizer.client;

/**
 * Contabilidad pura del presupuesto de particulas. Existe aparte del mixin para
 * poder probar los bordes sin arrancar Minecraft.
 *
 * <p>El contador incluye las particulas ya agrupadas, las que estan en la cola
 * {@code particlesToAdd} y los emisores de seguimiento. Si solo se mirasen los
 * grupos, una explosion podria llenar la cola pendiente sin limite hasta el
 * siguiente tick.</p>
 */
public final class ParticleBudget {
    private ParticleBudget() {
    }

    public static int used(int grouped, int pending, int trackingEmitters) {
        return Math.max(0, grouped) + Math.max(0, pending) + Math.max(0, trackingEmitters);
    }

    /** Un limite <= 0 significa "sin limite" para no romper configuraciones antiguas. */
    public static boolean isFull(int used, int limit) {
        return limit > 0 && used >= limit;
    }

    public static boolean isEmitterFull(int emitters, int limit) {
        return limit > 0 && emitters >= limit;
    }
}
