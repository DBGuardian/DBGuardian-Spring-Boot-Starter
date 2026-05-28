package io.dbguardian.core.loadbalance;

import io.dbguardian.core.datasource.DataSourceWrapper;
import io.dbguardian.model.RoutingContext;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SimpleRoundRobinStrategy implements ReadLoadBalanceStrategy {

    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public DataSourceWrapper select(List<DataSourceWrapper> availableSlaves, RoutingContext routingContext) {
        if (availableSlaves == null || availableSlaves.isEmpty()) {
            return null;
        }
        if (availableSlaves.size() == 1) {
            return availableSlaves.get(0);
        }
        int index = Math.abs(counter.getAndIncrement()) % availableSlaves.size();
        return availableSlaves.get(index);
    }

    @Override
    public String name() {
        return "round-robin";
    }

    @Override
    public String description() {
        return "简单轮询策略";
    }
}