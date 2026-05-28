package com.dbguardian.test;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
public class CoordinationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired(required = false)
    @Qualifier("datasourceCoordinationService")
    private Object coordinationService;

    @Test
    public void testCoordinationServiceLoaded() {
        assertNotNull(coordinationService);
    }

    @Test
    public void testCoordinationBeanExposed() {
        assertTrue(applicationContext.containsBean("datasourceCoordinationService"));
    }

    @Test
    public void testCoordinationBeanTypeNameStable() {
        assertEquals(
                "io.dbguardian.spring.coordination.DatasourceCoordinationService",
                applicationContext.getBean("datasourceCoordinationService").getClass().getName());
    }
}