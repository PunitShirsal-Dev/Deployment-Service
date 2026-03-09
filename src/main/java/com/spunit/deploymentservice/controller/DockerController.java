package com.spunit.deploymentservice.controller;

import com.spunit.deploymentservice.service.DockerService;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Info;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Docker Controller for container management operations
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/docker")
public class DockerController {

    private final DockerService dockerService;

    public DockerController(DockerService dockerService) {
        this.dockerService = dockerService;
    }

    /**
     * Build Docker image
     */
    @PostMapping("/build")
    public ResponseEntity<Map<String, String>> buildImage(
            @RequestParam String dockerfilePath,
            @RequestParam String imageName,
            @RequestParam String imageTag) {
        try {
            log.info("Building Docker image: {}:{}", imageName, imageTag);
            String imageId = dockerService.buildImage(dockerfilePath, imageName, imageTag);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "imageName", imageName + ":" + imageTag,
                    "imageId", imageId != null ? imageId : "N/A"
            ));
        } catch (Exception e) {
            log.error("Error building Docker image: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "status", "FAILED",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Push Docker image to registry
     */
    @PostMapping("/push")
    public ResponseEntity<Map<String, String>> pushImage(
            @RequestParam String imageName,
            @RequestParam String imageTag,
            @RequestParam String registryUrl) {
        try {
            log.info("Pushing Docker image: {} to registry: {}", imageName, registryUrl);
            dockerService.pushImage(imageName, imageTag, registryUrl);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "Image pushed successfully"
            ));
        } catch (Exception e) {
            log.error("Error pushing Docker image: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "status", "FAILED",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Create and start Docker container
     */
    @PostMapping("/container/create")
    public ResponseEntity<Map<String, String>> createContainer(
            @RequestParam String imageName,
            @RequestParam String imageTag,
            @RequestParam String containerName,
            @RequestParam(required = false) List<String> portBindings,
            @RequestParam(required = false) List<String> envVariables) {
        try {
            log.info("Creating Docker container: {}", containerName);
            String containerId = dockerService.createAndStartContainer(imageName, imageTag,
                    containerName, portBindings, envVariables);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "containerId", containerId,
                    "containerName", containerName
            ));
        } catch (Exception e) {
            log.error("Error creating Docker container: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "status", "FAILED",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Stop Docker container
     */
    @PostMapping("/container/{containerId}/stop")
    public ResponseEntity<Map<String, String>> stopContainer(
            @PathVariable String containerId) {
        try {
            log.info("Stopping Docker container: {}", containerId);
            dockerService.stopContainer(containerId);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "Container stopped successfully"
            ));
        } catch (Exception e) {
            log.error("Error stopping Docker container: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "status", "FAILED",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Remove Docker container
     */
    @DeleteMapping("/container/{containerId}")
    public ResponseEntity<Map<String, String>> removeContainer(
            @PathVariable String containerId) {
        try {
            log.info("Removing Docker container: {}", containerId);
            dockerService.removeContainer(containerId);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "Container removed successfully"
            ));
        } catch (Exception e) {
            log.error("Error removing Docker container: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "status", "FAILED",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * List all Docker containers
     */
    @GetMapping("/containers")
    public ResponseEntity<Object> listContainers(
            @RequestParam(defaultValue = "false") boolean showAll) {
        try {
            log.info("Listing Docker containers");
            List<Container> containers = dockerService.listContainers(showAll);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "count", containers.size(),
                    "containers", containers
            ));
        } catch (Exception e) {
            log.error("Error listing Docker containers: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "status", "FAILED",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get Docker container information
     */
    @GetMapping("/container/{containerId}")
    public ResponseEntity<Object> getContainerInfo(
            @PathVariable String containerId) {
        try {
            log.info("Getting Docker container info: {}", containerId);
            InspectContainerResponse containerInfo = dockerService.getContainerInfo(containerId);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "containerInfo", containerInfo
            ));
        } catch (Exception e) {
            log.error("Error getting Docker container info: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "status", "FAILED",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get Docker daemon information
     */
    @GetMapping("/info")
    public ResponseEntity<Object> getDockerInfo() {
        try {
            log.info("Getting Docker daemon info");
            Info dockerInfo = dockerService.getDockerInfo();
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "dockerInfo", dockerInfo
            ));
        } catch (Exception e) {
            log.error("Error getting Docker info: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "status", "FAILED",
                    "message", e.getMessage()
            ));
        }
    }
}

