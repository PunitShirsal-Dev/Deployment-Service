package com.spunit.deploymentservice.controller;

import com.spunit.deploymentservice.service.KubernetesService;
import io.fabric8.kubernetes.api.model.apps.DeploymentStatus;
import io.fabric8.kubernetes.api.model.Pod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Kubernetes Controller for container orchestration operations
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/kubernetes")
public class KubernetesController {

    private final KubernetesService kubernetesService;

    public KubernetesController(KubernetesService kubernetesService) {
        this.kubernetesService = kubernetesService;
    }

    /**
     * Create Kubernetes deployment
     */
    @PostMapping("/deployment/create")
    public ResponseEntity<Map<String, String>> createDeployment(
            @RequestParam String deploymentName,
            @RequestParam String imageName,
            @RequestParam(defaultValue = "3") Integer replicas,
            @RequestBody(required = false) Map<String, String> labels,
            @RequestBody(required = false) Map<String, String> envVars) {
        try {
            log.info("Creating Kubernetes deployment: {}", deploymentName);
            String deploymentId = kubernetesService.createDeployment(deploymentName, imageName,
                    replicas, labels, envVars);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "deploymentId", deploymentId,
                    "deploymentName", deploymentName
            ));
        } catch (Exception e) {
            log.error("Error creating Kubernetes deployment: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "status", "FAILED",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Update Kubernetes deployment
     */
    @PutMapping("/deployment/{deploymentName}")
    public ResponseEntity<Map<String, String>> updateDeployment(
            @PathVariable String deploymentName,
            @RequestParam String imageName,
            @RequestParam(defaultValue = "3") Integer replicas) {
        try {
            log.info("Updating Kubernetes deployment: {}", deploymentName);
            kubernetesService.updateDeployment(deploymentName, imageName, replicas);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "Deployment updated successfully"
            ));
        } catch (Exception e) {
            log.error("Error updating Kubernetes deployment: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "status", "FAILED",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Delete Kubernetes deployment
     */
    @DeleteMapping("/deployment/{deploymentName}")
    public ResponseEntity<Map<String, String>> deleteDeployment(
            @PathVariable String deploymentName) {
        try {
            log.info("Deleting Kubernetes deployment: {}", deploymentName);
            kubernetesService.deleteDeployment(deploymentName);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "Deployment deleted successfully"
            ));
        } catch (Exception e) {
            log.error("Error deleting Kubernetes deployment: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "status", "FAILED",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get Kubernetes deployment status
     */
    @GetMapping("/deployment/{deploymentName}/status")
    public ResponseEntity<Object> getDeploymentStatus(
            @PathVariable String deploymentName) {
        try {
            log.info("Getting Kubernetes deployment status: {}", deploymentName);
            DeploymentStatus status = kubernetesService.getDeploymentStatus(deploymentName);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "deploymentStatus", status
            ));
        } catch (Exception e) {
            log.error("Error getting Kubernetes deployment status: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "status", "FAILED",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Create Kubernetes service
     */
    @PostMapping("/service/create")
    public ResponseEntity<Map<String, String>> createService(
            @RequestParam String serviceName,
            @RequestParam String deploymentName,
            @RequestParam(defaultValue = "8080") Integer port,
            @RequestParam(defaultValue = "LoadBalancer") String serviceType) {
        try {
            log.info("Creating Kubernetes service: {}", serviceName);
            String serviceId = kubernetesService.createService(serviceName, deploymentName, port, serviceType);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "serviceId", serviceId,
                    "serviceName", serviceName
            ));
        } catch (Exception e) {
            log.error("Error creating Kubernetes service: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "status", "FAILED",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Delete Kubernetes service
     */
    @DeleteMapping("/service/{serviceName}")
    public ResponseEntity<Map<String, String>> deleteService(
            @PathVariable String serviceName) {
        try {
            log.info("Deleting Kubernetes service: {}", serviceName);
            kubernetesService.deleteService(serviceName);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "Service deleted successfully"
            ));
        } catch (Exception e) {
            log.error("Error deleting Kubernetes service: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "status", "FAILED",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * List pods for a deployment
     */
    @GetMapping("/deployment/{deploymentName}/pods")
    public ResponseEntity<Object> listPods(
            @PathVariable String deploymentName) {
        try {
            log.info("Listing pods for deployment: {}", deploymentName);
            List<Pod> pods = kubernetesService.listPods(deploymentName);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "count", pods.size(),
                    "pods", pods
            ));
        } catch (Exception e) {
            log.error("Error listing pods: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "status", "FAILED",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get pod logs
     */
    @GetMapping("/pod/{podName}/logs")
    public ResponseEntity<Object> getPodLogs(
            @PathVariable String podName) {
        try {
            log.info("Getting logs for pod: {}", podName);
            String logs = kubernetesService.getPodLogs(podName);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "podName", podName,
                    "logs", logs
            ));
        } catch (Exception e) {
            log.error("Error getting pod logs: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "status", "FAILED",
                    "message", e.getMessage()
            ));
        }
    }
}

