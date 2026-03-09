package com.spunit.deploymentservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Deployment result model
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeploymentResult {
    private String serviceName;
    private String status;  // SUCCESS, FAILED, IN_PROGRESS, ROLLED_BACK
    private String activeEnvironment;
    private String message;
    private Date timestamp;
    private String deploymentId;
    private Integer currentReplicas;
    private Integer desiredReplicas;
}

