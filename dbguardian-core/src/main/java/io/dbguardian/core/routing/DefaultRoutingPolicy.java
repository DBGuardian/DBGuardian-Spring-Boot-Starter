package io.dbguardian.core.routing;

import io.dbguardian.model.NodeModel;
import io.dbguardian.model.RoutingContext;
import io.dbguardian.spi.RoutingPolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultRoutingPolicy implements RoutingPolicy {

    private final AtomicInteger slaveCursor = new AtomicInteger(0);
    private final AtomicInteger masterCursor = new AtomicInteger(0);

    @Override
    public String getName() {
        return "default-routing-policy";
    }

    @Override
    public NodeModel select(List<NodeModel> candidates, RoutingContext context) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        List<NodeModel> masters = filterByRole(candidates, "master");
        List<NodeModel> slaves = filterByRole(candidates, "slave");

        boolean useMaster = shouldUseMaster(context, slaves);
        if (useMaster) {
            return chooseByWeight(masters.isEmpty() ? candidates : masters, masterCursor);
        }

        return chooseByWeight(slaves, slaveCursor);
    }

    private boolean shouldUseMaster(RoutingContext context, List<NodeModel> slaves) {
        if (context == null) {
            return slaves == null || slaves.isEmpty();
        }
        if (context.isForceMaster()) {
            return true;
        }
        if (context.isTransactional() && !context.isReadOnlyTransaction()) {
            return true;
        }
        String operation = context.getOperation();
        if (operation == null) {
            return slaves == null || slaves.isEmpty();
        }
        if (!"read".equalsIgnoreCase(operation)) {
            return true;
        }
        return slaves == null || slaves.isEmpty();
    }

    private List<NodeModel> filterByRole(List<NodeModel> candidates, String role) {
        List<NodeModel> results = new ArrayList<NodeModel>();
        for (NodeModel candidate : candidates) {
            if (candidate == null || !candidate.isEnabled()) {
                continue;
            }
            if (role.equalsIgnoreCase(candidate.getRole())) {
                results.add(candidate);
            }
        }
        return results;
    }

    private NodeModel chooseByWeight(List<NodeModel> candidates, AtomicInteger cursor) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        if (candidates.size() == 1) {
            return candidates.get(0);
        }

        int totalWeight = 0;
        for (NodeModel candidate : candidates) {
            totalWeight += Math.max(1, candidate.getWeight());
        }

        int position = Math.abs(cursor.getAndIncrement()) % totalWeight;
        int cumulative = 0;
        for (NodeModel candidate : candidates) {
            cumulative += Math.max(1, candidate.getWeight());
            if (position < cumulative) {
                return candidate;
            }
        }
        return candidates.get(candidates.size() - 1);
    }
}