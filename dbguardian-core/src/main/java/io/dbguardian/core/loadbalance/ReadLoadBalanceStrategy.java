package io.dbguardian.core.loadbalance;

import io.dbguardian.core.datasource.DataSourceWrapper;
import io.dbguardian.model.RoutingContext;

import java.util.List;

public interface ReadLoadBalanceStrategy {

    DataSourceWrapper select(List<DataSourceWrapper> availableSlaves, RoutingContext routingContext);

    String name();

    String description();
}