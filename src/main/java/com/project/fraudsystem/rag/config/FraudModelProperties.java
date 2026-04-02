package com.project.fraudsystem.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "fraud.ml")
public class FraudModelProperties {

    private String pythonExecutable = ".venv/bin/python";
    private String scoringScript = "scripts/inference/score_transaction.py";
    private String trainingConfig = "configs/train_config.yaml";
    private long timeoutMillis = 15000L;

    public String getPythonExecutable() {
        return pythonExecutable;
    }

    public void setPythonExecutable(String pythonExecutable) {
        this.pythonExecutable = pythonExecutable;
    }

    public String getScoringScript() {
        return scoringScript;
    }

    public void setScoringScript(String scoringScript) {
        this.scoringScript = scoringScript;
    }

    public String getTrainingConfig() {
        return trainingConfig;
    }

    public void setTrainingConfig(String trainingConfig) {
        this.trainingConfig = trainingConfig;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public void setTimeoutMillis(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }
}
