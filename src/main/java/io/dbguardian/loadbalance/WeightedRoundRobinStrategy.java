package io.dbguardian.loadbalance;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 加权轮询负载均衡策略
 * 
 * 特点：
 * - 按权重比例分配请求
 * - 请求分布均匀
 * - 适合从库性能一致的场景
 */
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

        // 计算总权重
        int totalWeight = availableSlaves.stream()
                .mapToInt(DataSourceWrapper::getWeight)
                .sum();

        if (totalWeight == 0) {
            // 权重都为0时，使用简单轮询
            int index = Math.abs(counter.getAndIncrement()) % availableSlaves.size();
            return availableSlaves.get(index);
        }

        // 使用加权轮询算法
        int index = Math.abs(counter.getAndIncrement()) % availableSlaves.size();
        DataSourceWrapper selected = availableSlaves.get(index);

        // 检查权重是否足够，当前选中节点的权重减1后是否还能满足
        // 这里使用简化的加权轮询：按顺序轮询，权重越大概率被选中
        return selectByWeight(availableSlaves, totalWeight);
    }

    private DataSourceWrapper selectByWeight(List<DataSourceWrapper> slaves, int totalWeight) {
        int cursor = Math.abs(counter.get()) % totalWeight;
        int cumulative = 0;

        for (DataSourceWrapper slave : slaves) {
            cumulative += slave.getWeight();
            if (cursor < cumulative) {
                return slave;
            }
        }

        // 理论上不会走到这里，但为了安全返回最后一个
        return slaves.get(slaves.size() - 1);
    }

    @Override
    public String name() {
        return "weighted-round-robin";
    }

    @Override
    public String description() {
        return "加权轮询策略，按权重比例分配请求，实现负载均衡";
    }
}
