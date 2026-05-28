package io.dbguardian.spi;

import io.dbguardian.model.NodeModel;
import io.dbguardian.model.RoutingContext;

import java.util.List;

public interface RoutingPolicy {

    String getName();

    NodeModel select(List<NodeModel> candidates, RoutingContext context);
}