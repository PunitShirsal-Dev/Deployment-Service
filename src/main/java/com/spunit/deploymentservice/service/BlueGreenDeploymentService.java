package com.spunit.deploymentservice.service;

import com.spunit.deploymentservice.model.DeploymentEnvironment;
import com.spunit.deploymentservice.model.DeploymentResult;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Blue-Green Deployment Strategy Service
 *
 * This service implements the blue-green deployment pattern where:
 * - Blue: Current production version
 * - Green: New version being deployed
 * - Switch happens instantly after health checks pass
 */
@Slf4j
@Service
public class BlueGreenDeploymentService {

    @Value("${deployment.deployment.strategies.blue-green.enabled:true}")
    private boolean enabled;

    @Value("${deployment.deployment.strategies.blue-green.health-check-interval:30}")
    private Integer healthCheckInterval;

    private final KubernetesService kubernetesService;
    private final Map<String, DeploymentEnvironment> activeEnvironments = new ConcurrentHashMap<>();

    public BlueGreenDeploymentService(KubernetesService kubernetesService) {
        this.kubernetesService = kubernetesService;
    }

    /**
     * Start blue-green deployment
     */
    @CircuitBreaker(name = "deploymentCircuitBreaker", fallbackMethod = "deployFallback")
    public DeploymentResult deploy(String serviceName, String newImageName, Integer replicas,
                                   Map<String, String> labels, Map<String, String> envVars) {
        try {
            log.info("Starting blue-green deployment for service: {}", serviceName);

            String blueEnvironment = serviceName + "-blue";
            String greenEnvironment = serviceName + "-green";

            // Check if blue environment exists
            DeploymentEnvironment current = activeEnvironments.get(serviceName);
            String activeEnv = current != null ? current.getActiveName() : blueEnvironment;
            String inactiveEnv = activeEnv.equals(blueEnvironment) ? greenEnvironment : blueEnvironment;

            log.info("Active environment: {}, Deploying to: {}", activeEnv, inactiveEnv);

            // Deploy to inactive environment
            String deploymentId = kubernetesService.createDeployment(
                    inactiveEnv,
                    newImageName,
                    replicas,
                    labels,
                    envVars
            );

            log.info("Deployment created in inactive environment: {}", deploymentId);

            // Health check
            boolean healthy = performHealthCheck(inactiveEnv, newImageName);

            if (healthy) {
                // Switch traffic
                switchTraffic(serviceName, activeEnv, inactiveEnv);

                // Update active environment tracking
                DeploymentEnvironment newEnv = new DeploymentEnvironment(inactiveEnv, newImageName,
                        replicas, System.currentTimeMillis());
                activeEnvironments.put(serviceName, newEnv);

                // Clean up old environment
                cleanupOldEnvironment(activeEnv);

                log.info("Blue-green deployment completed successfully for service: {}", serviceName);
                return DeploymentResult.builder()
                        .serviceName(serviceName)
                        .status("SUCCESS")
                        .activeEnvironment(inactiveEnv)
                        .message("Deployment successful, traffic switched to " + inactiveEnv)
                        .timestamp(new Date())
                        .build();
            } else {
                // Rollback on health check failure
                log.warn("Health check failed, rolling back deployment");
                kubernetesService.deleteDeployment(inactiveEnv);

                return DeploymentResult.builder()
                        .serviceName(serviceName)
                        .status("FAILED")
                        .activeEnvironment(activeEnv)
                        .message("Health check failed, deployment rolled back")
                        .timestamp(new Date())
                        .build();
            }
        } catch (Exception e) {
            log.error("Error in blue-green deployment: {}", e.getMessage(), e);
            throw new RuntimeException("Blue-green deployment failed", e);
        }
    }

    /**
     * Perform health checks on the new environment
     */
    private boolean performHealthCheck(String environmentName, String imageName) {
        try {
            log.info("Performing health check on environment: {}", environmentName);

            // Simulate health check - in production, you would call actual health endpoints
            Thread.sleep(healthCheckInterval * 1000L);

            log.info("Health check passed for environment: {}", environmentName);
            return true;
        } catch (Exception e) {
            log.error("Health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Switch traffic from active to new environment
     */
    private void switchTraffic(String serviceName, String oldEnv, String newEnv) {
        try {
            log.info("Switching traffic from {} to {}", oldEnv, newEnv);

            // Update service selector to point to new environment
            kubernetesService.deleteService(serviceName);
            kubernetesService.createService(serviceName, newEnv, 8080, "LoadBalancer");

            log.info("Traffic successfully switched to new environment: {}", newEnv);
        } catch (Exception e) {
            log.error("Error switching traffic: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to switch traffic", e);
        }
    }

    /**
     * Clean up old environment after successful deployment
     */
    private void cleanupOldEnvironment(String environmentName) {
        try {
            log.info("Cleaning up old environment: {}", environmentName);

            kubernetesService.deleteDeployment(environmentName);

            log.info("Old environment cleaned up: {}", environmentName);
        } catch (Exception e) {
            log.warn("Error cleaning up old environment: {}", e.getMessage());
        }
    }

    /**
     * Rollback to previous version
     */
    @CircuitBreaker(name = "deploymentCircuitBreaker", fallbackMethod = "rollbackFallback")
    public DeploymentResult rollback(String serviceName) {
        try {
            log.info("Rolling back deployment for service: {}", serviceName);

            DeploymentEnvironment current = activeEnvironments.get(serviceName);
            if (current == null) {
                throw new RuntimeException("No active deployment found");
            }

            String activeEnv = current.getActiveName();
            String previousEnv = activeEnv.equals(serviceName + "-blue") ?
                    serviceName + "-green" : serviceName + "-blue";

            // Switch back to previous environment
            switchTraffic(serviceName, activeEnv, previousEnv);

            log.info("Rollback completed for service: {}", serviceName);
            return DeploymentResult.builder()
                    .serviceName(serviceName)
                    .status("ROLLED_BACK")
                    .activeEnvironment(previousEnv)
                    .message("Deployment rolled back to previous version")
                    .timestamp(new Date())
                    .build();
        } catch (Exception e) {
            log.error("Error in rollback: {}", e.getMessage(), e);
            throw new RuntimeException("Rollback failed", e);
        }
    }

    public DeploymentResult deployFallback(String serviceName, String newImageName, Integer replicas,
                                          Map<String, String> labels, Map<String, String> envVars,
                                          Exception e) {
        log.error("Circuit breaker fallback for deploy: {}", e.getMessage());
        return DeploymentResult.builder()
                .serviceName(serviceName)
                .status("FAILED")
                .message("Deployment failed due to circuit breaker")
                .timestamp(new Date())
                .build();
    }

    public DeploymentResult rollbackFallback(String serviceName, Exception e) {
        log.error("Circuit breaker fallback for rollback: {}", e.getMessage());
        return DeploymentResult.builder()
                .serviceName(serviceName)
                .status("FAILED")
                .message("Rollback failed due to circuit breaker")
                .timestamp(new Date())
                .build();
    }
}

