package com.spunit.deploymentservice.controller;

import com.spunit.deploymentservice.service.HelmChartService;
import com.spunit.deploymentservice.model.HelmChart;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Helm Chart Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/helm")
public class HelmChartController {

    private final HelmChartService helmChartService;

    public HelmChartController(HelmChartService helmChartService) {
        this.helmChartService = helmChartService;
    }

    /**
     * Create Helm chart
     */
    @PostMapping("/chart/create")
    public ResponseEntity<Map<String, String>> createHelmChart(
            @RequestParam String chartName,
            @RequestParam String serviceName,
            @RequestParam String appVersion,
            @RequestBody(required = false) Map<String, Object> values) {
        try {
            log.info("Creating Helm chart: {}", chartName);
            String createdChart = helmChartService.createHelmChart(chartName, serviceName,
                    appVersion, values);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "chartName", createdChart,
                    "message", "Helm chart created successfully"
            ));
        } catch (Exception e) {
            log.error("Error creating Helm chart: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "status", "FAILED",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get Helm chart information
     */
    @GetMapping("/chart/{chartName}")
    public ResponseEntity<Object> getChartInfo(
            @PathVariable String chartName) {
        try {
            log.info("Getting Helm chart info: {}", chartName);
            HelmChart chart = helmChartService.getChartInfo(chartName);

            if (chart == null) {
                return ResponseEntity.status(404).body(Map.of(
                        "status", "NOT_FOUND",
                        "message", "Helm chart not found: " + chartName
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "chart", chart
            ));
        } catch (Exception e) {
            log.error("Error getting Helm chart info: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "status", "FAILED",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Install Helm chart to Kubernetes
     */
    @PostMapping("/chart/{chartName}/install")
    public ResponseEntity<Map<String, String>> installChart(
            @PathVariable String chartName,
            @RequestParam String releaseName,
            @RequestParam(defaultValue = "default") String namespace,
            @RequestBody(required = false) Map<String, String> overrideValues) {
        try {
            log.info("Installing Helm chart: {} with release: {}", chartName, releaseName);
            String release = helmChartService.installChart(chartName, releaseName, namespace,
                    overrideValues);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "releaseName", release,
                    "namespace", namespace,
                    "message", "Helm chart installed successfully"
            ));
        } catch (Exception e) {
            log.error("Error installing Helm chart: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "status", "FAILED",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Upgrade Helm chart
     */
    @PutMapping("/chart/{chartName}/upgrade")
    public ResponseEntity<Map<String, String>> upgradeChart(
            @PathVariable String chartName,
            @RequestParam String releaseName,
            @RequestParam(defaultValue = "default") String namespace,
            @RequestBody(required = false) Map<String, String> overrideValues) {
        try {
            log.info("Upgrading Helm chart: {} release: {}", chartName, releaseName);
            helmChartService.upgradeChart(releaseName, chartName, namespace, overrideValues);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "releaseName", releaseName,
                    "message", "Helm chart upgraded successfully"
            ));
        } catch (Exception e) {
            log.error("Error upgrading Helm chart: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "status", "FAILED",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Uninstall Helm chart
     */
    @DeleteMapping("/chart/{chartName}/uninstall")
    public ResponseEntity<Map<String, String>> uninstallChart(
            @PathVariable String chartName,
            @RequestParam String releaseName,
            @RequestParam(defaultValue = "default") String namespace) {
        try {
            log.info("Uninstalling Helm chart: {} release: {}", chartName, releaseName);
            helmChartService.uninstallChart(releaseName, namespace);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "releaseName", releaseName,
                    "message", "Helm chart uninstalled successfully"
            ));
        } catch (Exception e) {
            log.error("Error uninstalling Helm chart: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "status", "FAILED",
                    "message", e.getMessage()
            ));
        }
    }
}

