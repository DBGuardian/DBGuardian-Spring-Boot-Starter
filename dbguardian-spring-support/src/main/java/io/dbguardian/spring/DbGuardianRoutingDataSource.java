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
        if (context == null) {
            return MASTER_KEY;
        }
        boolean readOperation = context.getOperation() != null && "read".equalsIgnoreCase(context.getOperation());
        if (runtimeStateManager != null) {
            return runtimeStateManager.resolveLookupKey(context.isForceMaster(), readOperation);
        }
        if (context.isForceMaster()) {
            return MASTER_KEY;
        }
        return readOperation ? SLAVE_KEY : MASTER_KEY;
    }
}