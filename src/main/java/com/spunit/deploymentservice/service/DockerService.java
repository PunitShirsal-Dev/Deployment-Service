package com.spunit.deploymentservice.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.transport.DockerHttpClient;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Docker Service for container management operations
 */
@Slf4j
@Service
public class DockerService {

    @Value("${deployment.docker.enabled:true}")
    private boolean enabled;

    @Value("${deployment.docker.socket-uri:unix:///var/run/docker.sock}")
    private String socketUri;

    @Value("${deployment.docker.timeout:120}")
    private Integer timeout;

    private DockerClient dockerClient;

    public DockerService() {
        initializeDockerClient();
    }

    /**
     * Initialize Docker client with configuration
     */
    private void initializeDockerClient() {
        try {
            if (enabled) {
                DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                        .build();

                DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                        .dockerHost(java.net.URI.create(socketUri != null ? socketUri : "unix:///var/run/docker.sock"))
                        .connectionTimeout(Duration.ofSeconds(timeout != null ? timeout : 30))
                        .responseTimeout(Duration.ofSeconds(timeout != null ? timeout : 45))
                        .build();

                dockerClient = DockerClientImpl.getInstance(config, httpClient);

                log.info("Docker client initialized successfully");
            }
        } catch (Exception e) {
            log.warn("Failed to initialize Docker client: {}", e.getMessage());
        }
    }

    /**
     * Build Docker image from Dockerfile
     */
    @CircuitBreaker(name = "dockerClientCircuitBreaker", fallbackMethod = "buildImageFallback")
    public String buildImage(String dockerfilePath, String imageName, String imageTag) {
        try {
            log.info("Building Docker image: {}:{}", imageName, imageTag);

            String imageId = dockerClient.buildImageCmd()
                    .withDockerfile(new java.io.File(dockerfilePath))
                    .withTags(Set.of(imageName + ":" + imageTag))
                    .exec(new DockerBuildResponseCallback())
                    .awaitCompletion()
                    .toString();

            log.info("Docker image built successfully: {}", imageId);
            return imageId;
        } catch (Exception e) {
            log.error("Error building Docker image: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to build Docker image", e);
        }
    }

    /**
     * Push Docker image to registry
     */
    @CircuitBreaker(name = "dockerClientCircuitBreaker", fallbackMethod = "pushImageFallback")
    public void pushImage(String imageName, String imageTag, String registryUrl) {
        try {
            log.info("Pushing Docker image to registry: {}:{} -> {}", imageName, imageTag, registryUrl);

            dockerClient.pushImageCmd(registryUrl + "/" + imageName + ":" + imageTag)
                    .exec(new DockerPushResponseCallback())
                    .awaitCompletion();

            log.info("Docker image pushed successfully");
        } catch (Exception e) {
            log.error("Error pushing Docker image: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to push Docker image", e);
        }
    }

    /**
     * Create and start a Docker container
     */
    @CircuitBreaker(name = "dockerClientCircuitBreaker", fallbackMethod = "createContainerFallback")
    public String createAndStartContainer(String imageName, String imageTag, String containerName,
                                         List<String> portBindings, List<String> envVariables) {
        try {
            log.info("Creating Docker container: {}", containerName);

            CreateContainerResponse container = dockerClient.createContainerCmd(imageName + ":" + imageTag)
                    .withName(containerName)
                    .withEnv(envVariables)
                    .withPortBindings(portBindings.stream()
                            .map(PortBinding::parse)
                            .collect(Collectors.toList()))
                    .exec();

            String containerId = container.getId();
            log.info("Container created with ID: {}", containerId);

            dockerClient.startContainerCmd(containerId).exec();
            log.info("Container started successfully: {}", containerName);

            return containerId;
        } catch (Exception e) {
            log.error("Error creating Docker container: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create Docker container", e);
        }
    }

    /**
     * Stop a Docker container
     */
    @CircuitBreaker(name = "dockerClientCircuitBreaker", fallbackMethod = "stopContainerFallback")
    public void stopContainer(String containerId) {
        try {
            log.info("Stopping Docker container: {}", containerId);

            dockerClient.stopContainerCmd(containerId)
                    .withTimeout(30)
                    .exec();

            log.info("Container stopped successfully: {}", containerId);
        } catch (Exception e) {
            log.error("Error stopping Docker container: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to stop Docker container", e);
        }
    }

    /**
     * Remove a Docker container
     */
    @CircuitBreaker(name = "dockerClientCircuitBreaker", fallbackMethod = "removeContainerFallback")
    public void removeContainer(String containerId) {
        try {
            log.info("Removing Docker container: {}", containerId);

            dockerClient.removeContainerCmd(containerId)
                    .withForce(true)
                    .exec();

            log.info("Container removed successfully: {}", containerId);
        } catch (Exception e) {
            log.error("Error removing Docker container: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to remove Docker container", e);
        }
    }

    /**
     * List all Docker containers
     */
    @CircuitBreaker(name = "dockerClientCircuitBreaker", fallbackMethod = "listContainersFallback")
    public List<Container> listContainers(boolean showAll) {
        try {
            log.info("Listing Docker containers");

            return dockerClient.listContainersCmd()
                    .withShowAll(showAll)
                    .exec();
        } catch (Exception e) {
            log.error("Error listing Docker containers: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to list Docker containers", e);
        }
    }

    /**
     * Get Docker container information
     */
    @CircuitBreaker(name = "dockerClientCircuitBreaker", fallbackMethod = "getContainerInfoFallback")
    public InspectContainerResponse getContainerInfo(String containerId) {
        try {
            log.info("Getting Docker container info: {}", containerId);

            return dockerClient.inspectContainerCmd(containerId).exec();
        } catch (Exception e) {
            log.error("Error getting Docker container info: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get Docker container info", e);
        }
    }

    /**
     * Get Docker daemon info
     */
    @CircuitBreaker(name = "dockerClientCircuitBreaker", fallbackMethod = "getDockerInfoFallback")
    public Info getDockerInfo() {
        try {
            log.info("Getting Docker daemon info");

            return dockerClient.infoCmd().exec();
        } catch (Exception e) {
            log.error("Error getting Docker info: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get Docker info", e);
        }
    }

    // Fallback methods
    public String buildImageFallback(String dockerfilePath, String imageName, String imageTag,
                                    Exception e) {
        log.error("Circuit breaker fallback for buildImage: {}", e.getMessage());
        return null;
    }

    public void pushImageFallback(String imageName, String imageTag, String registryUrl,
                                 Exception e) {
        log.error("Circuit breaker fallback for pushImage: {}", e.getMessage());
    }

    public String createContainerFallback(String imageName, String imageTag, String containerName,
                                         List<String> portBindings, List<String> envVariables,
                                         Exception e) {
        log.error("Circuit breaker fallback for createContainer: {}", e.getMessage());
        return null;
    }

    public void stopContainerFallback(String containerId, Exception e) {
        log.error("Circuit breaker fallback for stopContainer: {}", e.getMessage());
    }

    public void removeContainerFallback(String containerId, Exception e) {
        log.error("Circuit breaker fallback for removeContainer: {}", e.getMessage());
    }

    public List<Container> listContainersFallback(boolean showAll, Exception e) {
        log.error("Circuit breaker fallback for listContainers: {}", e.getMessage());
        return List.of();
    }

    public InspectContainerResponse getContainerInfoFallback(String containerId, Exception e) {
        log.error("Circuit breaker fallback for getContainerInfo: {}", e.getMessage());
        return null;
    }

    public Info getDockerInfoFallback(Exception e) {
        log.error("Circuit breaker fallback for getDockerInfo: {}", e.getMessage());
        return null;
    }
}

class DockerBuildResponseCallback extends com.github.dockerjava.api.async.ResultCallback.Adapter<com.github.dockerjava.api.model.BuildResponseItem> {
    @Override
    public void onNext(com.github.dockerjava.api.model.BuildResponseItem item) {
        // Handle build response
    }
}

class DockerPushResponseCallback extends com.github.dockerjava.api.async.ResultCallback.Adapter<com.github.dockerjava.api.model.PushResponseItem> {
    @Override
    public void onNext(com.github.dockerjava.api.model.PushResponseItem item) {
        // Handle push response
    }
}

