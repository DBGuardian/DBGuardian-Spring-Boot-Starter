package io.dbguardian.spi;

import io.dbguardian.model.RoutingContext;

public interface OrmIntegration {

    String getName();

    boolean supports(String integrationType);

    void enrich(RoutingContext context);
}