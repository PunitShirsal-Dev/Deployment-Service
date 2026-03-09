package com.spunit.deploymentservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Canary deployment state model
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CanaryDeploymentState {
    private String serviceName;
    private String newImageName;
    private Integer replicas;
    private Integer currentTrafficPercentage;
    private Long deploymentStartTime;
    private Long lastShiftTime;
    private boolean completed;
    private boolean rolledBack;
}

