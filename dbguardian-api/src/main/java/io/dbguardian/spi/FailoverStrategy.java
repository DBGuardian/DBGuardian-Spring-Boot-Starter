package io.dbguardian.spi;

import io.dbguardian.model.ClusterModel;

public interface FailoverStrategy {

    String getName();

    void failover(ClusterModel clusterModel, String failedNodeId);
}