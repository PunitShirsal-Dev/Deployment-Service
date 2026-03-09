package com.spunit.deploymentservice.service;

import com.spunit.deploymentservice.model.CanaryDeploymentState;
import com.spunit.deploymentservice.model.DeploymentResult;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Canary Deployment Strategy Service
 *
 * This service implements the canary deployment pattern where:
 * - New version is deployed alongside current version
 * - Traffic is gradually shifted from old to new version
 * - Rollback can happen at any time during the process
 */
@Slf4j
@Service
public class CanaryDeploymentService {

    @Value("${deployment.deployment.strategies.canary.enabled:true}")
    private boolean enabled;

    @Value("${deployment.deployment.strategies.canary.initial-traffic:10}")
    private Integer initialTraffic;

    @Value("${deployment.deployment.strategies.canary.increment:10}")
    private Integer increment;

    @Value("${deployment.deployment.strategies.canary.interval:60}")
    private Integer interval;

    private final KubernetesService kubernetesService;
    private final Map<String, CanaryDeploymentState> activeCanaryDeployments = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);

    public CanaryDeploymentService(KubernetesService kubernetesService) {
        this.kubernetesService = kubernetesService;
    }

    /**
     * Start canary deployment
     */
    @CircuitBreaker(name = "deploymentCircuitBreaker", fallbackMethod = "deployFallback")
    public DeploymentResult deploy(String serviceName, String newImageName, Integer replicas,
                                   Map<String, String> labels, Map<String, String> envVars) {
        try {
            log.info("Starting canary deployment for service: {}", serviceName);

            String canaryName = serviceName + "-canary";

            // Deploy canary version
            String canaryId = kubernetesService.createDeployment(
                    canaryName,
                    newImageName,
                    1,  // Start with 1 replica for canary
                    labels,
                    envVars
            );

            log.info("Canary deployment created: {}", canaryId);

            // Initialize canary state
            CanaryDeploymentState canaryState = new CanaryDeploymentState(
                    serviceName,
                    newImageName,
                    replicas,
                    initialTraffic,
                    System.currentTimeMillis(),
                    System.currentTimeMillis(),
                    false,
                    false
            );
            activeCanaryDeployments.put(serviceName, canaryState);

            // Start traffic shifting schedule
            startTrafficShiftSchedule(serviceName);

            return DeploymentResult.builder()
                    .serviceName(serviceName)
                    .status("IN_PROGRESS")
                    .activeEnvironment(canaryName)
                    .message("Canary deployment started, traffic shifting to " + initialTraffic + "%")
                    .timestamp(new Date())
                    .build();
        } catch (Exception e) {
            log.error("Error in canary deployment: {}", e.getMessage(), e);
            throw new RuntimeException("Canary deployment failed", e);
        }
    }

    /**
     * Start scheduled traffic shifting
     */
    private void startTrafficShiftSchedule(String serviceName) {
        executor.scheduleAtFixedRate(() -> {
            try {
                CanaryDeploymentState state = activeCanaryDeployments.get(serviceName);
                if (state != null && !state.isCompleted()) {
                    shiftTraffic(serviceName, state);
                }
            } catch (Exception e) {
                log.error("Error during traffic shift schedule: {}", e.getMessage(), e);
            }
        }, interval, interval, TimeUnit.SECONDS);
    }

    /**
     * Shift traffic from stable to canary version
     */
    private void shiftTraffic(String serviceName, CanaryDeploymentState state) {
        try {
            int currentTraffic = state.getCurrentTrafficPercentage();
            int newTraffic = Math.min(currentTraffic + increment, 100);

            log.info("Shifting traffic for service {}: {} -> {}%",
                    serviceName, currentTraffic, newTraffic);

            // Update traffic weights in service mesh (simulated)
            updateServiceMeshWeights(serviceName, newTraffic);

            // Check health metrics
            boolean healthy = checkHealthMetrics(serviceName);

            if (!healthy) {
                log.warn("Health metrics degraded, rolling back canary deployment");
                rollback(serviceName);
                return;
            }

            state.setCurrentTrafficPercentage(newTraffic);
            state.setLastShiftTime(System.currentTimeMillis());

            if (newTraffic >= 100) {
                completeCanaryDeployment(serviceName);
            }

            log.info("Traffic shifted successfully. Current canary traffic: {}%", newTraffic);
        } catch (Exception e) {
            log.error("Error shifting traffic: {}", e.getMessage(), e);
        }
    }

    /**
     * Update service mesh weights (simulated)
     */
    private void updateServiceMeshWeights(String serviceName, int canaryTraffic) {
        try {
            log.info("Updating service mesh weights for {}: stable={}%, canary={}%",
                    serviceName, 100 - canaryTraffic, canaryTraffic);

            // In production, this would update Istio/Linkerd configurations
            // For now, we log the operation
        } catch (Exception e) {
            log.error("Error updating service mesh weights: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Check health metrics for canary deployment
     */
    private boolean checkHealthMetrics(String serviceName) {
        try {
            log.info("Checking health metrics for service: {}", serviceName);

            // Simulate health check - in production, you would check:
            // - Error rates
            // - Latency
            // - Resource usage
            // - Custom business metrics
            return true;
        } catch (Exception e) {
            log.error("Error checking health metrics: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Complete canary deployment (promote canary to stable)
     */
    private void completeCanaryDeployment(String serviceName) {
        try {
            log.info("Completing canary deployment for service: {}", serviceName);

            CanaryDeploymentState state = activeCanaryDeployments.get(serviceName);
            if (state != null) {
                // Update stable version to new image
                kubernetesService.updateDeployment(serviceName, state.getNewImageName(),
                        state.getReplicas());

                // Remove canary deployment
                kubernetesService.deleteDeployment(serviceName + "-canary");

                // Mark as completed
                state.setCompleted(true);

                log.info("Canary deployment completed successfully for service: {}", serviceName);
            }
        } catch (Exception e) {
            log.error("Error completing canary deployment: {}", e.getMessage(), e);
        }
    }

    /**
     * Rollback canary deployment
     */
    @CircuitBreaker(name = "deploymentCircuitBreaker", fallbackMethod = "rollbackFallback")
    public DeploymentResult rollback(String serviceName) {
        try {
            log.info("Rolling back canary deployment for service: {}", serviceName);

            // Remove canary deployment
            kubernetesService.deleteDeployment(serviceName + "-canary");

            // Mark as rolled back
            CanaryDeploymentState state = activeCanaryDeployments.get(serviceName);
            if (state != null) {
                state.setCompleted(true);
                state.setRolledBack(true);
            }

            log.info("Canary deployment rolled back for service: {}", serviceName);
            return DeploymentResult.builder()
                    .serviceName(serviceName)
                    .status("ROLLED_BACK")
                    .message("Canary deployment rolled back")
                    .timestamp(new Date())
                    .build();
        } catch (Exception e) {
            log.error("Error in rollback: {}", e.getMessage(), e);
            throw new RuntimeException("Rollback failed", e);
        }
    }

    /**
     * Get canary deployment status
     */
    public CanaryDeploymentState getCanaryStatus(String serviceName) {
        return activeCanaryDeployments.get(serviceName);
    }

    /**
     * Manual traffic shift
     */
    public void manualTrafficShift(String serviceName, Integer trafficPercentage) {
        try {
            log.info("Manual traffic shift for service {}: {}%", serviceName, trafficPercentage);

            CanaryDeploymentState state = activeCanaryDeployments.get(serviceName);
            if (state != null && !state.isCompleted()) {
                updateServiceMeshWeights(serviceName, trafficPercentage);
                state.setCurrentTrafficPercentage(trafficPercentage);
            }
        } catch (Exception e) {
            log.error("Error in manual traffic shift: {}", e.getMessage(), e);
            throw new RuntimeException("Manual traffic shift failed", e);
        }
    }

    public DeploymentResult deployFallback(String serviceName, String newImageName, Integer replicas,
                                          Map<String, String> labels, Map<String, String> envVars,
                                          Exception e) {
        log.error("Circuit breaker fallback for deploy: {}", e.getMessage());
        return DeploymentResult.builder()
                .serviceName(serviceName)
                .status("FAILED")
                .message("Canary deployment failed due to circuit breaker")
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

