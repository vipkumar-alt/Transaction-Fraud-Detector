package com.project.fraudsystem.rag.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.fraudsystem.rag.config.FraudModelProperties;
import com.project.fraudsystem.rag.dto.RagRequestDTO;
import com.project.fraudsystem.rag.model.FraudModelScore;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Service
public class FraudModelScoreService {

    private final FraudModelProperties properties;
    private final ObjectMapper objectMapper;

    public FraudModelScoreService(FraudModelProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public FraudModelScore score(RagRequestDTO request) {
        Path projectRoot = Path.of("").toAbsolutePath();
        String pythonExecutable = resolvePythonExecutable(projectRoot, properties.getPythonExecutable());
        Path scoringScript = resolvePath(projectRoot, properties.getScoringScript());
        Path trainingConfig = resolvePath(projectRoot, properties.getTrainingConfig());

        if (!Files.exists(scoringScript)) {
            throw new IllegalStateException("Fraud scoring script not found: " + scoringScript);
        }
        if (!Files.exists(trainingConfig)) {
            throw new IllegalStateException("Fraud model config not found: " + trainingConfig);
        }

        ProcessBuilder processBuilder = new ProcessBuilder(
                pythonExecutable,
                scoringScript.toString(),
                "--config",
                trainingConfig.toString()
        );
        processBuilder.directory(projectRoot.toFile());

        try {
            Process process = processBuilder.start();
            try (var stdin = process.getOutputStream()) {
                objectMapper.writeValue(stdin, request);
            }

            boolean finished = process.waitFor(properties.getTimeoutMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("Fraud model scoring timed out after " + properties.getTimeoutMillis() + "ms");
            }

            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            String errorOutput = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (process.exitValue() != 0) {
                throw new IllegalStateException("Fraud model scoring failed: " + errorOutput);
            }
            if (output.isBlank()) {
                throw new IllegalStateException("Fraud model scoring returned an empty response");
            }

            return objectMapper.readValue(output, FraudModelScore.class);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Fraud model scoring was interrupted", e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to execute fraud model scoring", e);
        }
    }

    private Path resolvePath(Path projectRoot, String configuredPath) {
        Path path = Path.of(configuredPath);
        if (path.isAbsolute()) {
            return path;
        }
        return projectRoot.resolve(path).normalize();
    }

    private String resolvePythonExecutable(Path projectRoot, String configuredExecutable) {
        Path configuredPath = Path.of(configuredExecutable);
        if (configuredPath.isAbsolute()) {
            return configuredPath.toString();
        }

        if (configuredExecutable.contains("/") || configuredExecutable.contains("\\")) {
            return projectRoot.resolve(configuredPath).normalize().toString();
        }

        Path localVenvPython = projectRoot.resolve(".venv/bin/python").normalize();
        if ("python3".equals(configuredExecutable) && Files.exists(localVenvPython)) {
            return localVenvPython.toString();
        }

        return configuredExecutable;
    }
}
