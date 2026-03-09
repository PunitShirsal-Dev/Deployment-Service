package com.spunit.deploymentservice.controller;

import com.spunit.deploymentservice.model.DeploymentResult;
import com.spunit.deploymentservice.service.BlueGreenDeploymentService;
import com.spunit.deploymentservice.service.CanaryDeploymentService;
import com.spunit.deploymentservice.model.CanaryDeploymentState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Deployment Controller for managing deployment strategies
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/deployment")
public class DeploymentController {

    private final BlueGreenDeploymentService blueGreenDeploymentService;
    private final CanaryDeploymentService canaryDeploymentService;

    public DeploymentController(BlueGreenDeploymentService blueGreenDeploymentService,
                               CanaryDeploymentService canaryDeploymentService) {
        this.blueGreenDeploymentService = blueGreenDeploymentService;
        this.canaryDeploymentService = canaryDeploymentService;
    }

    /**
     * Deploy using Blue-Green strategy
     */
    @PostMapping("/blue-green/deploy")
    public ResponseEntity<DeploymentResult> deployBlueGreen(
            @RequestParam String serviceName,
            @RequestParam String newImageName,
            @RequestParam(defaultValue = "3") Integer replicas,
            @RequestBody(required = false) Map<String, String> labels,
            @RequestBody(required = false) Map<String, String> envVars) {
        try {
            log.info("Starting blue-green deployment for service: {}", serviceName);
            DeploymentResult result = blueGreenDeploymentService.deploy(serviceName, newImageName,
                    replicas, labels, envVars);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error in blue-green deployment: {}", e.getMessage());
            return ResponseEntity.status(500).body(DeploymentResult.builder()
                    .serviceName(serviceName)
                    .status("FAILED")
                    .message(e.getMessage())
                    .build());
        }
    }

    /**
     * Rollback blue-green deployment
     */
    @PostMapping("/blue-green/{serviceName}/rollback")
    public ResponseEntity<DeploymentResult> rollbackBlueGreen(
            @PathVariable String serviceName) {
        try {
            log.info("Rolling back blue-green deployment for service: {}", serviceName);
            DeploymentResult result = blueGreenDeploymentService.rollback(serviceName);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error rolling back blue-green deployment: {}", e.getMessage());
            return ResponseEntity.status(500).body(DeploymentResult.builder()
                    .serviceName(serviceName)
                    .status("FAILED")
                    .message(e.getMessage())
                    .build());
        }
    }

    /**
     * Deploy using Canary strategy
     */
    @PostMapping("/canary/deploy")
    public ResponseEntity<DeploymentResult> deployCanary(
            @RequestParam String serviceName,
            @RequestParam String newImageName,
            @RequestParam(defaultValue = "3") Integer replicas,
            @RequestBody(required = false) Map<String, String> labels,
            @RequestBody(required = false) Map<String, String> envVars) {
        try {
            log.info("Starting canary deployment for service: {}", serviceName);
            DeploymentResult result = canaryDeploymentService.deploy(serviceName, newImageName,
                    replicas, labels, envVars);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error in canary deployment: {}", e.getMessage());
            return ResponseEntity.status(500).body(DeploymentResult.builder()
                    .serviceName(serviceName)
                    .status("FAILED")
                    .message(e.getMessage())
                    .build());
        }
    }

    /**
     * Get canary deployment status
     */
    @GetMapping("/canary/{serviceName}/status")
    public ResponseEntity<Object> getCanaryStatus(
            @PathVariable String serviceName) {
        try {
            log.info("Getting canary deployment status for service: {}", serviceName);
            CanaryDeploymentState state = canaryDeploymentService.getCanaryStatus(serviceName);
            if (state == null) {
                return ResponseEntity.status(404).body(Map.of(
                        "status", "NOT_FOUND",
                        "message", "Canary deployment not found for service: " + serviceName
                ));
            }
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "canaryState", state
            ));
        } catch (Exception e) {
            log.error("Error getting canary status: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "status", "FAILED",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Manual traffic shift for canary deployment
     */
    @PostMapping("/canary/{serviceName}/traffic")
    public ResponseEntity<Map<String, Object>> manualTrafficShift(
            @PathVariable String serviceName,
            @RequestParam Integer trafficPercentage) {
        try {
            log.info("Manual traffic shift for service: {} to {}%", serviceName, trafficPercentage);
            canaryDeploymentService.manualTrafficShift(serviceName, trafficPercentage);

            CanaryDeploymentState state = canaryDeploymentService.getCanaryStatus(serviceName);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "Traffic shifted to " + trafficPercentage + "%",
                    "canaryState", state
            ));
        } catch (Exception e) {
            log.error("Error in manual traffic shift: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "status", "FAILED",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Rollback canary deployment
     */
    @PostMapping("/canary/{serviceName}/rollback")
    public ResponseEntity<DeploymentResult> rollbackCanary(
            @PathVariable String serviceName) {
        try {
            log.info("Rolling back canary deployment for service: {}", serviceName);
            DeploymentResult result = canaryDeploymentService.rollback(serviceName);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error rolling back canary deployment: {}", e.getMessage());
            return ResponseEntity.status(500).body(DeploymentResult.builder()
                    .serviceName(serviceName)
                    .status("FAILED")
                    .message(e.getMessage())
                    .build());
        }
    }
}

