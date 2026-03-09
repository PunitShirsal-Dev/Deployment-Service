package com.spunit.deploymentservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Deployment Service Application
 *
 * This service provides comprehensive deployment management features including:
 * - Docker Containerization
 * - Docker Compose Setup
 * - Kubernetes Deployment
 * - Helm Chart Creation
 * - CI/CD Pipeline Integration
 * - Blue-Green Deployment
 * - Canary Deployment
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class DeploymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeploymentServiceApplication.class, args);
    }

}

