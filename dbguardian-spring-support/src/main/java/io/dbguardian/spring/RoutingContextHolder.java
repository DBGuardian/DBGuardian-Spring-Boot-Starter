package io.dbguardian.spring;

import io.dbguardian.model.RoutingContext;

public final class RoutingContextHolder {

    private static final ThreadLocal<RoutingContext> HOLDER = new ThreadLocal<RoutingContext>();

    private RoutingContextHolder() {
    }

    public static void set(RoutingContext context) {
        HOLDER.set(context);
    }

    public static RoutingContext get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}