package io.dbguardian.loadbalance;

import java.util.List;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;

/**
 * 一致性哈希负载均衡策略
 * 
 * 特点：
 * - 相同分片键始终路由到同一节点
 * - 节点增减时影响范围最小
 * - 适合需要会话保持的场景
 */
public class ConsistentHashStrategy implements ReadLoadBalanceStrategy {

    private static final int VIRTUAL_NODES = 100;
    private final NavigableMap<Long, DataSourceWrapper> ring = new ConcurrentSkipListMap<>();
    private volatile List<DataSourceWrapper> currentSlaves;

    @Override
    public DataSourceWrapper select(List<DataSourceWrapper> availableSlaves, RoutingContext routingContext) {
        if (availableSlaves == null || availableSlaves.isEmpty()) {
            return null;
        }

        if (availableSlaves.size() == 1) {
            return availableSlaves.get(0);
        }

        // 如果节点列表发生变化，重建哈希环
        if (hasChanged(availableSlaves)) {
            rebuildRing(availableSlaves);
        }

        // 获取分片键
        String shardKey = routingContext != null ? routingContext.getShardKey() : null;
        if (shardKey == null) {
            // 没有分片键时，使用随机值
            shardKey = String.valueOf(System.nanoTime());
        }

        // 计算哈希值
        long hash = hash(shardKey);

        // 在哈希环上顺时针查找节点
        Long key = ring.ceilingKey(hash);
        if (key == null) {
            // 回到环的开头
            key = ring.firstKey();
        }

        return ring.get(key);
    }

    /**
     * 判断节点列表是否发生变化
     */
    private boolean hasChanged(List<DataSourceWrapper> slaves) {
        if (currentSlaves == null || currentSlaves.size() != slaves.size()) {
            return true;
        }

        List<String> currentIds = currentSlaves.stream()
                .map(DataSourceWrapper::getId)
                .sorted()
                .collect(Collectors.toList());

        List<String> newIds = slaves.stream()
                .map(DataSourceWrapper::getId)
                .sorted()
                .collect(Collectors.toList());

        return !currentIds.equals(newIds);
    }

    /**
     * 重建哈希环
     */
    private synchronized void rebuildRing(List<DataSourceWrapper> slaves) {
        ring.clear();

        for (DataSourceWrapper slave : slaves) {
            // 为每个物理节点创建多个虚拟节点
            for (int i = 0; i < VIRTUAL_NODES; i++) {
                long hash = hash(slave.getId() + "-" + i);
                ring.put(hash, slave);
            }
        }

        currentSlaves = slaves;
    }

    /**
     * 计算哈希值
     * 使用 String.hashCode() 的变体，确保正数结果
     */
    private long hash(String key) {
        long h = 0;
        for (int i = 0; i < key.length(); i++) {
            h = 31 * h + key.charAt(i);
        }
        // 确保返回正数
        return h & 0x7fffffffffffffffL;
    }

    @Override
    public String name() {
        return "consistent-hash";
    }

    @Override
    public String description() {
        return "一致性哈希策略，相同分片键始终路由到同一节点，节点变化时影响最小";
    }
}
