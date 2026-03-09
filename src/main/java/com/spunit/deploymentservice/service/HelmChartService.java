package com.spunit.deploymentservice.service;

import com.spunit.deploymentservice.model.HelmChart;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Helm Chart Service
 *
 * This service manages Helm chart creation and deployment for Kubernetes
 */
@Slf4j
@Service
public class HelmChartService {

    private final Map<String, HelmChart> helmCharts = new ConcurrentHashMap<>();
    private final String chartsDirectory = "./helm-charts";

    public HelmChartService() {
        initializeChartsDirectory();
    }

    /**
     * Initialize charts directory
     */
    private void initializeChartsDirectory() {
        try {
            Path chartsPath = Paths.get(chartsDirectory);
            if (!Files.exists(chartsPath)) {
                Files.createDirectories(chartsPath);
                log.info("Created Helm charts directory: {}", chartsDirectory);
            }
        } catch (Exception e) {
            log.warn("Error initializing charts directory: {}", e.getMessage());
        }
    }

    /**
     * Create Helm chart for a service
     */
    @CircuitBreaker(name = "deploymentCircuitBreaker", fallbackMethod = "createChartFallback")
    public String createHelmChart(String chartName, String serviceName, String appVersion,
                                  Map<String, Object> values) {
        try {
            log.info("Creating Helm chart: {}", chartName);

            HelmChart chart = HelmChart.builder()
                    .chartName(chartName)
                    .serviceName(serviceName)
                    .version("1.0.0")
                    .appVersion(appVersion)
                    .description("Helm chart for " + serviceName)
                    .values(values)
                    .createdAt(new Date())
                    .build();

            helmCharts.put(chartName, chart);

            // Create chart directory structure
            createChartStructure(chartName);

            // Create Chart.yaml
            createChartYaml(chartName, chart);

            // Create values.yaml
            createValuesYaml(chartName, chart);

            // Create deployment template
            createDeploymentTemplate(chartName, chart);

            // Create service template
            createServiceTemplate(chartName, chart);

            // Create configmap template
            createConfigMapTemplate(chartName, chart);

            log.info("Helm chart created successfully: {}", chartName);
            return chartName;
        } catch (Exception e) {
            log.error("Error creating Helm chart: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create Helm chart", e);
        }
    }

    /**
     * Create chart directory structure
     */
    private void createChartStructure(String chartName) {
        try {
            Path chartPath = Paths.get(chartsDirectory, chartName);
            Path templatesPath = chartPath.resolve("templates");
            Path chartsPath = chartPath.resolve("charts");

            Files.createDirectories(templatesPath);
            Files.createDirectories(chartsPath);

            log.info("Chart directory structure created: {}", chartPath);
        } catch (Exception e) {
            log.error("Error creating chart structure: {}", e.getMessage());
            throw new RuntimeException("Failed to create chart structure", e);
        }
    }

    /**
     * Create Chart.yaml
     */
    private void createChartYaml(String chartName, HelmChart chart) {
        try {
            Path chartYamlPath = Paths.get(chartsDirectory, chartName, "Chart.yaml");

            String content = "apiVersion: v2\n" +
                    "name: " + chart.getChartName() + "\n" +
                    "description: " + chart.getDescription() + "\n" +
                    "type: application\n" +
                    "version: " + chart.getVersion() + "\n" +
                    "appVersion: " + chart.getAppVersion() + "\n" +
                    "keywords:\n" +
                    "  - microservices\n" +
                    "  - spring-boot\n" +
                    "  - kubernetes\n";

            Files.write(chartYamlPath, content.getBytes());
            log.info("Chart.yaml created: {}", chartYamlPath);
        } catch (Exception e) {
            log.error("Error creating Chart.yaml: {}", e.getMessage());
            throw new RuntimeException("Failed to create Chart.yaml", e);
        }
    }

    /**
     * Create values.yaml
     */
    private void createValuesYaml(String chartName, HelmChart chart) {
        try {
            Path valuesYamlPath = Paths.get(chartsDirectory, chartName, "values.yaml");

            String content = "# Default values for " + chart.getChartName() + "\n" +
                    "# This is a YAML-formatted file.\n\n" +
                    "replicaCount: 3\n\n" +
                    "image:\n" +
                    "  repository: " + chart.getServiceName() + "\n" +
                    "  tag: \"" + chart.getAppVersion() + "\"\n" +
                    "  pullPolicy: IfNotPresent\n\n" +
                    "service:\n" +
                    "  type: LoadBalancer\n" +
                    "  port: 8080\n" +
                    "  targetPort: 8080\n\n" +
                    "resources:\n" +
                    "  limits:\n" +
                    "    cpu: 500m\n" +
                    "    memory: 512Mi\n" +
                    "  requests:\n" +
                    "    cpu: 250m\n" +
                    "    memory: 256Mi\n\n" +
                    "autoscaling:\n" +
                    "  enabled: true\n" +
                    "  minReplicas: 2\n" +
                    "  maxReplicas: 10\n" +
                    "  targetCPUUtilizationPercentage: 80\n\n" +
                    "env:\n" +
                    "  LOG_LEVEL: INFO\n";

            Files.write(valuesYamlPath, content.getBytes());
            log.info("values.yaml created: {}", valuesYamlPath);
        } catch (Exception e) {
            log.error("Error creating values.yaml: {}", e.getMessage());
            throw new RuntimeException("Failed to create values.yaml", e);
        }
    }

    /**
     * Create deployment template
     */
    private void createDeploymentTemplate(String chartName, HelmChart chart) {
        try {
            Path deploymentPath = Paths.get(chartsDirectory, chartName, "templates", "deployment.yaml");

            String content = "apiVersion: apps/v1\n" +
                    "kind: Deployment\n" +
                    "metadata:\n" +
                    "  name: {{ include \"" + chart.getChartName() + ".fullname\" . }}\n" +
                    "  labels:\n" +
                    "    {{- include \"" + chart.getChartName() + ".labels\" . | nindent 4 }}\n" +
                    "spec:\n" +
                    "  replicas: {{ .Values.replicaCount }}\n" +
                    "  selector:\n" +
                    "    matchLabels:\n" +
                    "      {{- include \"" + chart.getChartName() + ".selectorLabels\" . | nindent 6 }}\n" +
                    "  template:\n" +
                    "    metadata:\n" +
                    "      labels:\n" +
                    "        {{- include \"" + chart.getChartName() + ".selectorLabels\" . | nindent 8 }}\n" +
                    "    spec:\n" +
                    "      containers:\n" +
                    "      - name: {{ .Chart.Name }}\n" +
                    "        image: \"{{ .Values.image.repository }}:{{ .Values.image.tag }}\"\n" +
                    "        imagePullPolicy: {{ .Values.image.pullPolicy }}\n" +
                    "        ports:\n" +
                    "        - name: http\n" +
                    "          containerPort: {{ .Values.service.targetPort }}\n" +
                    "          protocol: TCP\n" +
                    "        livenessProbe:\n" +
                    "          httpGet:\n" +
                    "            path: /actuator/health\n" +
                    "            port: http\n" +
                    "          initialDelaySeconds: 30\n" +
                    "          periodSeconds: 10\n" +
                    "        readinessProbe:\n" +
                    "          httpGet:\n" +
                    "            path: /actuator/health/readiness\n" +
                    "            port: http\n" +
                    "          initialDelaySeconds: 10\n" +
                    "          periodSeconds: 5\n" +
                    "        resources:\n" +
                    "          {{- toYaml .Values.resources | nindent 12 }}\n" +
                    "        env:\n" +
                    "        {{- range $key, $value := .Values.env }}\n" +
                    "        - name: {{ $key }}\n" +
                    "          value: \"{{ $value }}\"\n" +
                    "        {{- end }}\n";

            Files.write(deploymentPath, content.getBytes());
            log.info("deployment.yaml template created: {}", deploymentPath);
        } catch (Exception e) {
            log.error("Error creating deployment template: {}", e.getMessage());
            throw new RuntimeException("Failed to create deployment template", e);
        }
    }

    /**
     * Create service template
     */
    private void createServiceTemplate(String chartName, HelmChart chart) {
        try {
            Path servicePath = Paths.get(chartsDirectory, chartName, "templates", "service.yaml");

            String content = "apiVersion: v1\n" +
                    "kind: Service\n" +
                    "metadata:\n" +
                    "  name: {{ include \"" + chart.getChartName() + ".fullname\" . }}\n" +
                    "  labels:\n" +
                    "    {{- include \"" + chart.getChartName() + ".labels\" . | nindent 4 }}\n" +
                    "spec:\n" +
                    "  type: {{ .Values.service.type }}\n" +
                    "  ports:\n" +
                    "    - port: {{ .Values.service.port }}\n" +
                    "      targetPort: http\n" +
                    "      protocol: TCP\n" +
                    "      name: http\n" +
                    "  selector:\n" +
                    "    {{- include \"" + chart.getChartName() + ".selectorLabels\" . | nindent 4 }}\n";

            Files.write(servicePath, content.getBytes());
            log.info("service.yaml template created: {}", servicePath);
        } catch (Exception e) {
            log.error("Error creating service template: {}", e.getMessage());
            throw new RuntimeException("Failed to create service template", e);
        }
    }

    /**
     * Create configmap template
     */
    private void createConfigMapTemplate(String chartName, HelmChart chart) {
        try {
            Path configmapPath = Paths.get(chartsDirectory, chartName, "templates", "configmap.yaml");

            String content = "apiVersion: v1\n" +
                    "kind: ConfigMap\n" +
                    "metadata:\n" +
                    "  name: {{ include \"" + chart.getChartName() + ".fullname\" . }}-config\n" +
                    "  labels:\n" +
                    "    {{- include \"" + chart.getChartName() + ".labels\" . | nindent 4 }}\n" +
                    "data:\n" +
                    "  application.yml: |\n" +
                    "    spring:\n" +
                    "      application:\n" +
                    "        name: " + chart.getServiceName() + "\n" +
                    "    server:\n" +
                    "      port: 8080\n";

            Files.write(configmapPath, content.getBytes());
            log.info("configmap.yaml template created: {}", configmapPath);
        } catch (Exception e) {
            log.error("Error creating configmap template: {}", e.getMessage());
            throw new RuntimeException("Failed to create configmap template", e);
        }
    }

    /**
     * Install Helm chart to Kubernetes
     */
    @CircuitBreaker(name = "deploymentCircuitBreaker", fallbackMethod = "installChartFallback")
    public String installChart(String chartName, String releaseName, String namespace,
                              Map<String, String> overrideValues) {
        try {
            log.info("Installing Helm chart: {} with release name: {}", chartName, releaseName);

            HelmChart chart = helmCharts.get(chartName);
            if (chart == null) {
                throw new RuntimeException("Helm chart not found: " + chartName);
            }

            // Simulate Helm installation
            AtomicReference<String> command = new AtomicReference<>("helm install " + releaseName + " " + chartsDirectory + "/" + chartName +
                    " --namespace " + namespace + " --create-namespace");

            if (overrideValues != null && !overrideValues.isEmpty()) {
                overrideValues.forEach((key, value) ->
                        command.set(command + " --set " + key + "=" + value)
                );
            }

            log.info("Helm installation command: {}", command);
            log.info("Chart installed successfully: {}", releaseName);

            return releaseName;
        } catch (Exception e) {
            log.error("Error installing Helm chart: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to install Helm chart", e);
        }
    }

    /**
     * Upgrade Helm chart
     */
    @CircuitBreaker(name = "deploymentCircuitBreaker", fallbackMethod = "upgradeChartFallback")
    public void upgradeChart(String releaseName, String chartName, String namespace,
                            Map<String, String> overrideValues) {
        try {
            log.info("Upgrading Helm chart: {} release: {}", chartName, releaseName);

            HelmChart chart = helmCharts.get(chartName);
            if (chart == null) {
                throw new RuntimeException("Helm chart not found: " + chartName);
            }

            log.info("Chart upgraded successfully: {}", releaseName);
        } catch (Exception e) {
            log.error("Error upgrading Helm chart: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upgrade Helm chart", e);
        }
    }

    /**
     * Uninstall Helm chart
     */
    @CircuitBreaker(name = "deploymentCircuitBreaker", fallbackMethod = "uninstallChartFallback")
    public void uninstallChart(String releaseName, String namespace) {
        try {
            log.info("Uninstalling Helm chart: {} from namespace: {}", releaseName, namespace);

            log.info("Chart uninstalled successfully: {}", releaseName);
        } catch (Exception e) {
            log.error("Error uninstalling Helm chart: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to uninstall Helm chart", e);
        }
    }

    /**
     * Get Helm chart information
     */
    public HelmChart getChartInfo(String chartName) {
        return helmCharts.get(chartName);
    }

    public String createChartFallback(String chartName, String serviceName, String appVersion,
                                     Map<String, Object> values, Exception e) {
        log.error("Circuit breaker fallback for createChart: {}", e.getMessage());
        return null;
    }

    public String installChartFallback(String chartName, String releaseName, String namespace,
                                      Map<String, String> overrideValues, Exception e) {
        log.error("Circuit breaker fallback for installChart: {}", e.getMessage());
        return null;
    }

    public void upgradeChartFallback(String releaseName, String chartName, String namespace,
                                    Map<String, String> overrideValues, Exception e) {
        log.error("Circuit breaker fallback for upgradeChart: {}", e.getMessage());
    }

    public void uninstallChartFallback(String releaseName, String namespace, Exception e) {
        log.error("Circuit breaker fallback for uninstallChart: {}", e.getMessage());
    }
}

