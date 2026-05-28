package io.dbguardian.spi;

import io.dbguardian.model.NodeModel;

public interface HealthIndicator {

    String getName();

    boolean isHealthy(NodeModel nodeModel);
}