package com.project.fraudsystem.rag.model;

public class FraudModelScore {

    private String modelName;
    private Double fraudScore;
    private Double fraudThreshold;
    private String modelDecision;

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public Double getFraudScore() {
        return fraudScore;
    }

    public void setFraudScore(Double fraudScore) {
        this.fraudScore = fraudScore;
    }

    public Double getFraudThreshold() {
        return fraudThreshold;
    }

    public void setFraudThreshold(Double fraudThreshold) {
        this.fraudThreshold = fraudThreshold;
    }

    public String getModelDecision() {
        return modelDecision;
    }

    public void setModelDecision(String modelDecision) {
        this.modelDecision = modelDecision;
    }
}
