package com.spunit.deploymentservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Stage Execution model
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StageExecution {
    private String stageName;
    private String status;          // SUCCESS, FAILED, RUNNING
    private String output;
    private Long duration;          // in seconds
}

