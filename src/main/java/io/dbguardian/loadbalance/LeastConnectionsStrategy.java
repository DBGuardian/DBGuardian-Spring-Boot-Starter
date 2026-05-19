package io.dbguardian.loadbalance;

import java.util.Comparator;
import java.util.List;

/**
 * 最少连接负载均衡策略
 * 
 * 特点：
 * - 选择当前活跃连接数最少的后端
 * - 适合长连接场景
 * - 能更好处理连接数不均匀的情况
 */
public class LeastConnectionsStrategy implements ReadLoadBalanceStrategy {

    @Override
    public DataSourceWrapper select(List<DataSourceWrapper> availableSlaves, RoutingContext routingContext) {
        if (availableSlaves == null || availableSlaves.isEmpty()) {
            return null;
        }

        if (availableSlaves.size() == 1) {
            return availableSlaves.get(0);
        }

        // 按活跃连接数升序排序
        return availableSlaves.stream()
                .min(Comparator.comparingInt(DataSourceWrapper::getActiveConnections))
                .orElse(availableSlaves.get(0));
    }

    @Override
    public String name() {
        return "least-connections";
    }

    @Override
    public String description() {
        return "最少连接策略，选择当前活跃连接数最少的后端服务器";
    }
}
