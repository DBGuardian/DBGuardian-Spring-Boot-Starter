package io.dbguardian.core.loadbalance;

import io.dbguardian.core.datasource.DataSourceWrapper;
import io.dbguardian.model.RoutingContext;

import java.util.List;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;

public class ConsistentHashStrategy implements ReadLoadBalanceStrategy {

    private static final int VIRTUAL_NODES = 100;

    private final NavigableMap<Long, DataSourceWrapper> ring = new ConcurrentSkipListMap<Long, DataSourceWrapper>();
    private volatile List<DataSourceWrapper> currentSlaves;

    @Override
    public DataSourceWrapper select(List<DataSourceWrapper> availableSlaves, RoutingContext routingContext) {
        if (availableSlaves == null || availableSlaves.isEmpty()) {
            return null;
        }
        if (availableSlaves.size() == 1) {
            return availableSlaves.get(0);
        }
        if (hasChanged(availableSlaves)) {
            rebuildRing(availableSlaves);
        }

        String shardKey = routingContext != null ? routingContext.getShardKey() : null;
        if (shardKey == null) {
            shardKey = String.valueOf(System.nanoTime());
        }

        long hash = hash(shardKey);
        Long nodeKey = ring.ceilingKey(hash);
        if (nodeKey == null) {
            nodeKey = ring.firstKey();
        }
        return ring.get(nodeKey);
    }

    private boolean hasChanged(List<DataSourceWrapper> slaves) {
        if (currentSlaves == null || currentSlaves.size() != slaves.size()) {
            return true;
        }
        List<String> currentIds = currentSlaves.stream().map(DataSourceWrapper::getId).sorted().collect(Collectors.toList());
        List<String> newIds = slaves.stream().map(DataSourceWrapper::getId).sorted().collect(Collectors.toList());
        return !currentIds.equals(newIds);
    }

    private synchronized void rebuildRing(List<DataSourceWrapper> slaves) {
        ring.clear();
        for (DataSourceWrapper slave : slaves) {
            for (int i = 0; i < VIRTUAL_NODES; i++) {
                ring.put(hash(slave.getId() + "-" + i), slave);
            }
        }
        currentSlaves = slaves;
    }

    private long hash(String key) {
        long value = 0L;
        for (int i = 0; i < key.length(); i++) {
            value = 31 * value + key.charAt(i);
        }
        return value & 0x7fffffffffffffffL;
    }

    @Override
    public String name() {
        return "consistent-hash";
    }

    @Override
    public String description() {
        return "一致性哈希策略";
    }
}