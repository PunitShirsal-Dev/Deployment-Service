package com.spunit.deploymentservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pipeline Execution model
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PipelineExecution {
    private String executionId;
    private String serviceName;
    private String commitId;
    private String commitMessage;
    private String status;           // RUNNING, SUCCESS, FAILED
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Map<String, Object> stages = new LinkedHashMap<>();
}

