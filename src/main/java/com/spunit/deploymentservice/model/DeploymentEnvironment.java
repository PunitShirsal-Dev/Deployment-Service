package com.spunit.deploymentservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Deployment environment model for tracking blue-green deployments
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeploymentEnvironment {
    private String activeName;      // e.g., service-blue or service-green
    private String imageName;
    private Integer replicas;
    private Long deploymentTime;
}

