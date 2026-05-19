package io.dbguardian.loadbalance;

import java.util.List;
import java.util.Random;

/**
 * 加权随机负载均衡策略
 * 
 * 特点：
 * - 按权重比例随机选择
 * - 实现简单，效果均匀
 * - 适合对延迟敏感的场景
 */
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

        // 计算总权重
        int totalWeight = availableSlaves.stream()
                .mapToInt(DataSourceWrapper::getWeight)
                .sum();

        if (totalWeight == 0) {
            // 权重都为0时，使用均匀随机
            return availableSlaves.get(random.nextInt(availableSlaves.size()));
        }

        // 加权随机选择
        int rand = random.nextInt(totalWeight);
        int cumulative = 0;

        for (DataSourceWrapper slave : availableSlaves) {
            cumulative += slave.getWeight();
            if (rand < cumulative) {
                return slave;
            }
        }

        // 理论上不会走到这里，但为了安全返回最后一个
        return availableSlaves.get(availableSlaves.size() - 1);
    }

    @Override
    public String name() {
        return "weighted-random";
    }

    @Override
    public String description() {
        return "加权随机策略，按权重比例随机选择请求目标";
    }
}
