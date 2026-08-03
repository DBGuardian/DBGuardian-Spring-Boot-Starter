package io.dbguardian.spring;

import io.dbguardian.model.RoutingContext;
import io.dbguardian.spring.runtime.ClusterRuntimeStateManager;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class DbGuardianRoutingDataSource extends AbstractRoutingDataSource {

    public static final String MASTER_KEY = "master";
    public static final String SLAVE_KEY = "slave";

    private ClusterRuntimeStateManager runtimeStateManager;

    public void setRuntimeStateManager(ClusterRuntimeStateManager runtimeStateManager) {
        this.runtimeStateManager = runtimeStateManager;
    }

    @Override
    protected Object determineCurrentLookupKey() {
        RoutingContext context = RoutingContextHolder.get();
        if (runtimeStateManager != null) {
            if (context == null) {
                return runtimeStateManager.resolveLookupKey(true, false);
            }
            boolean readOperation = context.getOperation() != null && "read".equalsIgnoreCase(context.getOperation());
            return runtimeStateManager.resolveLookupKey(context.isForceMaster(), readOperation);
        }
        if (context == null || context.isForceMaster()) {
            return MASTER_KEY;
        }
        boolean readOperation = context.getOperation() != null && "read".equalsIgnoreCase(context.getOperation());
        return readOperation ? SLAVE_KEY : MASTER_KEY;
    }
}