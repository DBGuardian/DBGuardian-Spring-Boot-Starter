package com.dbguardian.test;

import io.dbguardian.model.RoutingContext;
import io.dbguardian.spring.RoutingContextHolder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DataSourceContextHolderTest {

    @Test
    public void testContextSwitch() {
        assertNull(RoutingContextHolder.get());

        RoutingContext masterContext = new RoutingContext();
        masterContext.setOperation("write");
        RoutingContextHolder.set(masterContext);
        assertEquals("write", RoutingContextHolder.get().getOperation());

        RoutingContext slaveContext = new RoutingContext();
        slaveContext.setOperation("read");
        RoutingContextHolder.set(slaveContext);
        assertEquals("read", RoutingContextHolder.get().getOperation());

        RoutingContextHolder.clear();
        assertNull(RoutingContextHolder.get());
    }

    @Test
    public void testThreadIsolation() throws InterruptedException {
        RoutingContext context = new RoutingContext();
        context.setOperation("write");
        RoutingContextHolder.set(context);

        Thread newThread = new Thread(new Runnable() {
            @Override
            public void run() {
                assertNull(RoutingContextHolder.get());
                RoutingContext readContext = new RoutingContext();
                readContext.setOperation("read");
                RoutingContextHolder.set(readContext);
                assertEquals("read", RoutingContextHolder.get().getOperation());
                RoutingContextHolder.clear();
            }
        });

        newThread.start();
        newThread.join();

        assertEquals("write", RoutingContextHolder.get().getOperation());
        RoutingContextHolder.clear();
    }
}