package io.dbguardian.core;

import io.dbguardian.spi.CoordinationDriver;
import io.dbguardian.spi.DatabaseDialect;
import io.dbguardian.spi.FailoverStrategy;
import io.dbguardian.spi.HealthIndicator;
import io.dbguardian.spi.OrmIntegration;
import io.dbguardian.spi.ReadWriteClassifier;
import io.dbguardian.spi.RoutingPolicy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CapabilityRegistry {

    private final List<DatabaseDialect> databaseDialects = new ArrayList<DatabaseDialect>();
    private final List<ReadWriteClassifier> readWriteClassifiers = new ArrayList<ReadWriteClassifier>();
    private final List<RoutingPolicy> routingPolicies = new ArrayList<RoutingPolicy>();
    private final List<CoordinationDriver> coordinationDrivers = new ArrayList<CoordinationDriver>();
    private final List<OrmIntegration> ormIntegrations = new ArrayList<OrmIntegration>();
    private final List<FailoverStrategy> failoverStrategies = new ArrayList<FailoverStrategy>();
    private final List<HealthIndicator> healthIndicators = new ArrayList<HealthIndicator>();

    public void registerDatabaseDialect(DatabaseDialect dialect) {
        this.databaseDialects.add(dialect);
    }

    public void registerReadWriteClassifier(ReadWriteClassifier classifier) {
        this.readWriteClassifiers.add(classifier);
    }

    public void registerRoutingPolicy(RoutingPolicy policy) {
        this.routingPolicies.add(policy);
    }

    public void registerCoordinationDriver(CoordinationDriver driver) {
        this.coordinationDrivers.add(driver);
    }

    public void registerOrmIntegration(OrmIntegration integration) {
        this.ormIntegrations.add(integration);
    }

    public void registerFailoverStrategy(FailoverStrategy strategy) {
        this.failoverStrategies.add(strategy);
    }

    public void registerHealthIndicator(HealthIndicator indicator) {
        this.healthIndicators.add(indicator);
    }

    public List<DatabaseDialect> getDatabaseDialects() {
        return Collections.unmodifiableList(databaseDialects);
    }

    public List<ReadWriteClassifier> getReadWriteClassifiers() {
        return Collections.unmodifiableList(readWriteClassifiers);
    }

    public List<RoutingPolicy> getRoutingPolicies() {
        return Collections.unmodifiableList(routingPolicies);
    }

    public List<CoordinationDriver> getCoordinationDrivers() {
        return Collections.unmodifiableList(coordinationDrivers);
    }

    public List<OrmIntegration> getOrmIntegrations() {
        return Collections.unmodifiableList(ormIntegrations);
    }

    public List<FailoverStrategy> getFailoverStrategies() {
        return Collections.unmodifiableList(failoverStrategies);
    }

    public List<HealthIndicator> getHealthIndicators() {
        return Collections.unmodifiableList(healthIndicators);
    }
}