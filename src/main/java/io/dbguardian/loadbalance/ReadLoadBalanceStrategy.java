package io.dbguardian.loadbalance;

import java.util.List;

/**
 * 读操作负载均衡策略接口
 */
public interface ReadLoadBalanceStrategy {

    /**
     * 选择一个从库
     *
     * @param availableSlaves 可用的从库列表
     * @param routingContext 路由上下文（包含分片键等）
     * @return 选中的从库，如果无可用返回null
     */
    DataSourceWrapper select(List<DataSourceWrapper> availableSlaves, RoutingContext routingContext);

    /**
     * 策略名称
     */
    String name();

    /**
     * 策略描述
     */
    String description();
}
