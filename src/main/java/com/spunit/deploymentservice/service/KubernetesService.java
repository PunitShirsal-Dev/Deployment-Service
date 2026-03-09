package com.spunit.deploymentservice.service;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentStatus;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Kubernetes Service for container orchestration operations
 */
@Slf4j
@org.springframework.stereotype.Service
public class KubernetesService {

    @Value("${deployment.kubernetes.enabled:true}")
    private boolean enabled;

    @Value("${deployment.kubernetes.namespace:default}")
    private String namespace;

    private KubernetesClient kubernetesClient;

    public KubernetesService() {
        initializeKubernetesClient();
    }

    /**
     * Initialize Kubernetes client
     */
    private void initializeKubernetesClient() {
        try {
            if (enabled) {
                kubernetesClient = new KubernetesClientBuilder().build();
                log.info("Kubernetes client initialized successfully for namespace: {}", namespace);
            }
        } catch (Exception e) {
            log.warn("Failed to initialize Kubernetes client: {}", e.getMessage());
        }
    }

    /**
     * Create a Kubernetes deployment
     */
    @CircuitBreaker(name = "kubernetesClientCircuitBreaker", fallbackMethod = "createDeploymentFallback")
    public String createDeployment(String deploymentName, String imageName, Integer replicas,
                                   Map<String, String> labels, Map<String, String> envVars) {
        try {
            log.info("Creating Kubernetes deployment: {}", deploymentName);

            Map<String, String> deploymentLabels = new HashMap<>(labels);
            deploymentLabels.put("app", deploymentName);

            PodSpec podSpec = new PodSpecBuilder()
                    .addNewContainer()
                    .withName(deploymentName)
                    .withImage(imageName)
                    .withImagePullPolicy("IfNotPresent")
                    .addNewPort()
                    .withContainerPort(8080)
                    .endPort()
                    .endContainer()
                    .build();

            if (envVars != null && !envVars.isEmpty()) {
                envVars.forEach((key, value) ->
                    podSpec.getContainers().get(0).getEnv().add(
                        new EnvVarBuilder().withName(key).withValue(value).build()
                    )
                );
            }

            Deployment deployment = new DeploymentBuilder()
                    .withNewMetadata()
                    .withName(deploymentName)
                    .withNamespace(namespace)
                    .withLabels(deploymentLabels)
                    .endMetadata()
                    .withNewSpec()
                    .withReplicas(replicas)
                    .withNewSelector()
                    .addToMatchLabels("app", deploymentName)
                    .endSelector()
                    .withNewTemplate()
                    .withNewMetadata()
                    .addToLabels("app", deploymentName)
                    .endMetadata()
                    .withSpec(podSpec)
                    .endTemplate()
                    .endSpec()
                    .build();

            Deployment created = kubernetesClient.apps().deployments()
                    .inNamespace(namespace)
                    .resource(deployment)
                    .create();

            log.info("Deployment created successfully: {}", created.getMetadata().getName());
            return created.getMetadata().getUid();
        } catch (Exception e) {
            log.error("Error creating Kubernetes deployment: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create Kubernetes deployment", e);
        }
    }

    /**
     * Update a Kubernetes deployment
     */
    @CircuitBreaker(name = "kubernetesClientCircuitBreaker", fallbackMethod = "updateDeploymentFallback")
    public void updateDeployment(String deploymentName, String imageName, Integer replicas) {
        try {
            log.info("Updating Kubernetes deployment: {}", deploymentName);

            kubernetesClient.apps().deployments()
                    .inNamespace(namespace)
                    .withName(deploymentName)
                    .edit(d -> new DeploymentBuilder(d)
                            .editSpec()
                            .withReplicas(replicas)
                            .editTemplate()
                            .editSpec()
                            .editFirstContainer()
                            .withImage(imageName)
                            .endContainer()
                            .endSpec()
                            .endTemplate()
                            .endSpec()
                            .build());

            log.info("Deployment updated successfully: {}", deploymentName);
        } catch (Exception e) {
            log.error("Error updating Kubernetes deployment: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update Kubernetes deployment", e);
        }
    }

    /**
     * Delete a Kubernetes deployment
     */
    @CircuitBreaker(name = "kubernetesClientCircuitBreaker", fallbackMethod = "deleteDeploymentFallback")
    public void deleteDeployment(String deploymentName) {
        try {
            log.info("Deleting Kubernetes deployment: {}", deploymentName);

            kubernetesClient.apps().deployments()
                    .inNamespace(namespace)
                    .withName(deploymentName)
                    .delete();

            log.info("Deployment deleted successfully: {}", deploymentName);
        } catch (Exception e) {
            log.error("Error deleting Kubernetes deployment: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete Kubernetes deployment", e);
        }
    }

    /**
     * Get Kubernetes deployment status
     */
    @CircuitBreaker(name = "kubernetesClientCircuitBreaker", fallbackMethod = "getDeploymentStatusFallback")
    public DeploymentStatus getDeploymentStatus(String deploymentName) {
        try {
            log.info("Getting Kubernetes deployment status: {}", deploymentName);

            Deployment deployment = kubernetesClient.apps().deployments()
                    .inNamespace(namespace)
                    .withName(deploymentName)
                    .get();

            if (deployment != null) {
                return deployment.getStatus();
            }
            return null;
        } catch (Exception e) {
            log.error("Error getting Kubernetes deployment status: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get Kubernetes deployment status", e);
        }
    }

    /**
     * Create a Kubernetes service
     */
    @CircuitBreaker(name = "kubernetesClientCircuitBreaker", fallbackMethod = "createServiceFallback")
    public String createService(String serviceName, String deploymentName, Integer port,
                               String serviceType) {
        try {
            log.info("Creating Kubernetes service: {}", serviceName);

            io.fabric8.kubernetes.api.model.Service service = new ServiceBuilder()
                    .withNewMetadata()
                    .withName(serviceName)
                    .withNamespace(namespace)
                    .addToLabels("app", deploymentName)
                    .endMetadata()
                    .withNewSpec()
                    .withType(serviceType != null ? serviceType : "ClusterIP")
                    .addToSelector("app", deploymentName)
                    .addNewPort()
                    .withPort(port)
                    .withTargetPort(new io.fabric8.kubernetes.api.model.IntOrString(8080))
                    .endPort()
                    .endSpec()
                    .build();

            io.fabric8.kubernetes.api.model.Service created = kubernetesClient.services()
                    .inNamespace(namespace)
                    .resource(service)
                    .create();

            log.info("Service created successfully: {}", created.getMetadata().getName());
            return created.getMetadata().getUid();
        } catch (Exception e) {
            log.error("Error creating Kubernetes service: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create Kubernetes service", e);
        }
    }

    /**
     * Delete a Kubernetes service
     */
    @CircuitBreaker(name = "kubernetesClientCircuitBreaker", fallbackMethod = "deleteServiceFallback")
    public void deleteService(String serviceName) {
        try {
            log.info("Deleting Kubernetes service: {}", serviceName);

            kubernetesClient.services()
                    .inNamespace(namespace)
                    .withName(serviceName)
                    .delete();

            log.info("Service deleted successfully: {}", serviceName);
        } catch (Exception e) {
            log.error("Error deleting Kubernetes service: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete Kubernetes service", e);
        }
    }

    /**
     * Get list of pods for a deployment
     */
    @CircuitBreaker(name = "kubernetesClientCircuitBreaker", fallbackMethod = "listPodsFallback")
    public List<Pod> listPods(String deploymentName) {
        try {
            log.info("Listing pods for deployment: {}", deploymentName);

            return kubernetesClient.pods()
                    .inNamespace(namespace)
                    .withLabel("app", deploymentName)
                    .list()
                    .getItems();
        } catch (Exception e) {
            log.error("Error listing pods: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to list pods", e);
        }
    }

    /**
     * Get pod logs
     */
    @CircuitBreaker(name = "kubernetesClientCircuitBreaker", fallbackMethod = "getPodLogsFallback")
    public String getPodLogs(String podName) {
        try {
            log.info("Getting logs for pod: {}", podName);

            return kubernetesClient.pods()
                    .inNamespace(namespace)
                    .withName(podName)
                    .getLog();
        } catch (Exception e) {
            log.error("Error getting pod logs: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get pod logs", e);
        }
    }

    // Fallback methods
    public String createDeploymentFallback(String deploymentName, String imageName, Integer replicas,
                                          Map<String, String> labels, Map<String, String> envVars,
                                          Exception e) {
        log.error("Circuit breaker fallback for createDeployment: {}", e.getMessage());
        return null;
    }

    public void updateDeploymentFallback(String deploymentName, String imageName, Integer replicas,
                                        Exception e) {
        log.error("Circuit breaker fallback for updateDeployment: {}", e.getMessage());
    }

    public void deleteDeploymentFallback(String deploymentName, Exception e) {
        log.error("Circuit breaker fallback for deleteDeployment: {}", e.getMessage());
    }

    public DeploymentStatus getDeploymentStatusFallback(String deploymentName, Exception e) {
        log.error("Circuit breaker fallback for getDeploymentStatus: {}", e.getMessage());
        return null;
    }

    public String createServiceFallback(String serviceName, String deploymentName, Integer port,
                                       String serviceType, Exception e) {
        log.error("Circuit breaker fallback for createService: {}", e.getMessage());
        return null;
    }

    public void deleteServiceFallback(String serviceName, Exception e) {
        log.error("Circuit breaker fallback for deleteService: {}", e.getMessage());
    }

    public List<Pod> listPodsFallback(String deploymentName, Exception e) {
        log.error("Circuit breaker fallback for listPods: {}", e.getMessage());
        return List.of();
    }

    public String getPodLogsFallback(String podName, Exception e) {
        log.error("Circuit breaker fallback for getPodLogs: {}", e.getMessage());
        return null;
    }
}

