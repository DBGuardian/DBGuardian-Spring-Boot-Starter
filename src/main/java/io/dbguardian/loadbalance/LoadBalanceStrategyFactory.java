package io.dbguardian.loadbalance;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 负载均衡策略工厂
 * 根据配置创建和管理负载均衡策略
 */
@Component
public class LoadBalanceStrategyFactory {

    private final Map<String, ReadLoadBalanceStrategy> strategies = new ConcurrentHashMap<>();

    public LoadBalanceStrategyFactory() {
        // 注册默认策略
        register(new WeightedRoundRobinStrategy());
        register(new WeightedRandomStrategy());
        register(new LeastConnectionsStrategy());
        register(new ConsistentHashStrategy());
        // 简单轮询
        register(new SimpleRoundRobinStrategy());
    }

    /**
     * 注册策略
     */
    public void register(ReadLoadBalanceStrategy strategy) {
        strategies.put(strategy.name(), strategy);
    }

    /**
     * 获取策略
     */
    public ReadLoadBalanceStrategy getStrategy(String name) {
        ReadLoadBalanceStrategy strategy = strategies.get(name);
        if (strategy == null) {
            // 默认返回加权轮询
            return strategies.get("weighted-round-robin");
        }
        return strategy;
    }

    /**
     * 获取所有策略
     */
    public Map<String, ReadLoadBalanceStrategy> getAllStrategies() {
        return new HashMap<>(strategies);
    }

    /**
     * 检查策略是否存在
     */
    public boolean hasStrategy(String name) {
        return strategies.containsKey(name);
    }
}
