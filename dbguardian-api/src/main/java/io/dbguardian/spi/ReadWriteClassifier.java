package io.dbguardian.spi;

import io.dbguardian.model.RoutingContext;

public interface ReadWriteClassifier {

    boolean supports(RoutingContext context);

    String classify(RoutingContext context);
}