package io.dbguardian.spi;

public interface DatabaseDialect {

    String getName();

    String getDatabaseType();

    String getHealthCheckSql();
}