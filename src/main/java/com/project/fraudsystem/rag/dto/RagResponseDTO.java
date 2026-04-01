package com.project.fraudsystem.rag.dto;

import java.util.List;

public class RagResponseDTO {

    private String query;
    private List<String> retrievedTitles;
    private List<String> retrievedContents;
    private Double fraudScore;
    private Double fraudThreshold;
    private String fraudModelName;
    private String fraudModelDecision;
    private String explanation;
    private String riskLevel;
    private String recommendedAction;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public List<String> getRetrievedTitles() {
        return retrievedTitles;
    }

    public void setRetrievedTitles(List<String> retrievedTitles) {
        this.retrievedTitles = retrievedTitles;
    }

    public List<String> getRetrievedContents() {
        return retrievedContents;
    }

    public void setRetrievedContents(List<String> retrievedContents) {
        this.retrievedContents = retrievedContents;
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

    public String getFraudModelName() {
        return fraudModelName;
    }

    public void setFraudModelName(String fraudModelName) {
        this.fraudModelName = fraudModelName;
    }

    public String getFraudModelDecision() {
        return fraudModelDecision;
    }

    public void setFraudModelDecision(String fraudModelDecision) {
        this.fraudModelDecision = fraudModelDecision;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public void setRecommendedAction(String recommendedAction) {
        this.recommendedAction = recommendedAction;
    }
}
