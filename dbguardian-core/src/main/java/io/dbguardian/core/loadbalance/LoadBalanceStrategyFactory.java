package io.dbguardian.core.loadbalance;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LoadBalanceStrategyFactory {

    private final Map<String, ReadLoadBalanceStrategy> strategies = new ConcurrentHashMap<String, ReadLoadBalanceStrategy>();

    public LoadBalanceStrategyFactory() {
        register(new WeightedRoundRobinStrategy());
        register(new WeightedRandomStrategy());
        register(new LeastConnectionsStrategy());
        register(new ConsistentHashStrategy());
        register(new SimpleRoundRobinStrategy());
    }

    public void register(ReadLoadBalanceStrategy strategy) {
        strategies.put(strategy.name(), strategy);
    }

    public ReadLoadBalanceStrategy getStrategy(String name) {
        ReadLoadBalanceStrategy strategy = strategies.get(name);
        return strategy != null ? strategy : strategies.get("weighted-round-robin");
    }

    public Map<String, ReadLoadBalanceStrategy> getAllStrategies() {
        return new HashMap<String, ReadLoadBalanceStrategy>(strategies);
    }

    public boolean hasStrategy(String name) {
        return strategies.containsKey(name);
    }
}