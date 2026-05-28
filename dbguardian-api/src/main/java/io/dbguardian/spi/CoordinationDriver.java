package io.dbguardian.spi;

public interface CoordinationDriver {

    String getName();

    void publishTopologyChange(String message);
}