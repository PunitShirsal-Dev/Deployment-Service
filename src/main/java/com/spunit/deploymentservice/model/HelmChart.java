package com.spunit.deploymentservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.Map;

/**
 * Helm Chart model
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HelmChart {
    private String chartName;
    private String serviceName;
    private String version;
    private String appVersion;
    private String description;
    private Map<String, Object> values;
    private Date createdAt;
}

