package com.spunit.deploymentservice.controller;

import com.spunit.deploymentservice.model.PipelineExecution;
import com.spunit.deploymentservice.service.CIPipelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * CI/CD Pipeline Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ci-cd")
public class CIPipelineController {

    private final CIPipelineService ciPipelineService;

    public CIPipelineController(CIPipelineService ciPipelineService) {
        this.ciPipelineService = ciPipelineService;
    }

    /**
     * Create pipeline configuration
     */
    @PostMapping("/pipeline/config")
    public ResponseEntity<Map<String, String>> createPipelineConfiguration(
            @RequestParam String serviceName,
            @RequestParam String repository,
            @RequestParam String branch,
            @RequestParam String buildCommand,
            @RequestParam String testCommand,
            @RequestParam(defaultValue = "BLUE_GREEN") String deploymentStrategy) {
        try {
            log.info("Creating pipeline configuration for service: {}", serviceName);
            String configId = ciPipelineService.createPipelineConfiguration(serviceName, repository,
                    branch, buildCommand, testCommand, deploymentStrategy);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "configId", configId,
                    "message", "Pipeline configuration created successfully"
            ));
        } catch (Exception e) {
            log.error("Error creating pipeline configuration: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "status", "FAILED",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Trigger CI/CD pipeline
     */
    @PostMapping("/pipeline/trigger")
    public ResponseEntity<Map<String, Object>> triggerPipeline(
            @RequestParam String serviceName,
            @RequestParam String commitId,
            @RequestParam String commitMessage) {
        try {
            log.info("Triggering CI/CD pipeline for service: {}", serviceName);
            String executionId = ciPipelineService.triggerPipeline(serviceName, commitId, commitMessage);

            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "executionId", executionId,
                    "message", "Pipeline execution started"
            ));
        } catch (Exception e) {
            log.error("Error triggering pipeline: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "status", "FAILED",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get pipeline execution status
     */
    @GetMapping("/pipeline/{executionId}/status")
    public ResponseEntity<Object> getPipelineStatus(
            @PathVariable String executionId) {
        try {
            log.info("Getting pipeline status for execution: {}", executionId);
            PipelineExecution execution = ciPipelineService.getPipelineStatus(executionId);

            if (execution == null) {
                return ResponseEntity.status(404).body(Map.of(
                        "status", "NOT_FOUND",
                        "message", "Pipeline execution not found: " + executionId
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "execution", execution
            ));
        } catch (Exception e) {
            log.error("Error getting pipeline status: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "status", "FAILED",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get pipeline history for a service
     */
    @GetMapping("/pipeline/history/{serviceName}")
    public ResponseEntity<Object> getPipelineHistory(
            @PathVariable String serviceName) {
        try {
            log.info("Getting pipeline history for service: {}", serviceName);
            List<PipelineExecution> history = ciPipelineService.getPipelineHistory(serviceName);

            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "count", history.size(),
                    "executions", history
            ));
        } catch (Exception e) {
            log.error("Error getting pipeline history: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "status", "FAILED",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Retry failed pipeline
     */
    @PostMapping("/pipeline/{executionId}/retry")
    public ResponseEntity<Map<String, Object>> retryPipeline(
            @PathVariable String executionId) {
        try {
            log.info("Retrying pipeline execution: {}", executionId);
            String newExecutionId = ciPipelineService.retryPipeline(executionId);

            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "newExecutionId", newExecutionId,
                    "message", "Pipeline execution retried"
            ));
        } catch (Exception e) {
            log.error("Error retrying pipeline: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "status", "FAILED",
                    "message", e.getMessage()
            ));
        }
    }
}

