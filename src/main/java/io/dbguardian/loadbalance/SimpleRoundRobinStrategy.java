package io.dbguardian.loadbalance;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 简单轮询负载均衡策略
 * 
 * 特点：
 * - 无权重轮询，每个节点轮流处理
 * - 实现最简单
 * - 适合所有节点性能相同的场景
 */
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
        return "简单轮询策略，每个节点轮流处理请求";
    }
}
