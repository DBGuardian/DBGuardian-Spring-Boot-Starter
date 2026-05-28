package io.dbguardian.core.loadbalance;

import io.dbguardian.core.datasource.DataSourceWrapper;
import io.dbguardian.model.RoutingContext;

import java.util.Comparator;
import java.util.List;

public class LeastConnectionsStrategy implements ReadLoadBalanceStrategy {

    @Override
    public DataSourceWrapper select(List<DataSourceWrapper> availableSlaves, RoutingContext routingContext) {
        if (availableSlaves == null || availableSlaves.isEmpty()) {
            return null;
        }
        if (availableSlaves.size() == 1) {
            return availableSlaves.get(0);
        }
        return availableSlaves.stream()
                .min(Comparator.comparingInt(DataSourceWrapper::getActiveConnections))
                .orElse(availableSlaves.get(0));
    }

    @Override
    public String name() {
        return "least-connections";
    }

    @Override
    public String description() {
        return "最少连接策略";
    }
}