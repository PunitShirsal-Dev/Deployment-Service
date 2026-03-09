package com.spunit.deploymentservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class DeploymentServiceApplicationTest {

    @Test
    public void contextLoads() {
        assertNotNull(DeploymentServiceApplication.class);
    }

}

