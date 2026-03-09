package com.spunit.deploymentservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Pipeline Configuration model
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PipelineConfiguration {
    private String serviceName;
    private String repository;
    private String branch;
    private String buildCommand;
    private String testCommand;
    private String deploymentStrategy;   // BLUE_GREEN, CANARY, ROLLING
    private LocalDateTime createdAt;
}

