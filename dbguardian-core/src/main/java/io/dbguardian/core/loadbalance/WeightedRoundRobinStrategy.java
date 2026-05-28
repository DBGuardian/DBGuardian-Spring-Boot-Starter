package io.dbguardian.core.loadbalance;

import io.dbguardian.core.datasource.DataSourceWrapper;
import io.dbguardian.model.RoutingContext;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class WeightedRoundRobinStrategy implements ReadLoadBalanceStrategy {

    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public DataSourceWrapper select(List<DataSourceWrapper> availableSlaves, RoutingContext routingContext) {
        if (availableSlaves == null || availableSlaves.isEmpty()) {
            return null;
        }
        if (availableSlaves.size() == 1) {
            return availableSlaves.get(0);
        }

        int totalWeight = availableSlaves.stream().mapToInt(DataSourceWrapper::getWeight).sum();
        if (totalWeight <= 0) {
            int index = Math.abs(counter.getAndIncrement()) % availableSlaves.size();
            return availableSlaves.get(index);
        }

        int cursor = Math.abs(counter.getAndIncrement()) % totalWeight;
        int cumulative = 0;
        for (DataSourceWrapper slave : availableSlaves) {
            cumulative += slave.getWeight();
            if (cursor < cumulative) {
                return slave;
            }
        }
        return availableSlaves.get(availableSlaves.size() - 1);
    }

    @Override
    public String name() {
        return "weighted-round-robin";
    }

    @Override
    public String description() {
        return "加权轮询策略";
    }
}