package com.project.fraudsystem.rag.dto;

public class RagAnalysisStartResponseDTO {

    private String analysisId;

    public RagAnalysisStartResponseDTO() {
    }

    public RagAnalysisStartResponseDTO(String analysisId) {
        this.analysisId = analysisId;
    }

    public String getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }
}
