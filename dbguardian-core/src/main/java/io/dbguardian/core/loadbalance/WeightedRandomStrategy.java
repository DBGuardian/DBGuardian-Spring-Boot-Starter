package io.dbguardian.core.loadbalance;

import io.dbguardian.core.datasource.DataSourceWrapper;
import io.dbguardian.model.RoutingContext;

import java.util.List;
import java.util.Random;

public class WeightedRandomStrategy implements ReadLoadBalanceStrategy {

    private final Random random = new Random();

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
            return availableSlaves.get(random.nextInt(availableSlaves.size()));
        }

        int cursor = random.nextInt(totalWeight);
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
        return "weighted-random";
    }

    @Override
    public String description() {
        return "加权随机策略";
    }
}