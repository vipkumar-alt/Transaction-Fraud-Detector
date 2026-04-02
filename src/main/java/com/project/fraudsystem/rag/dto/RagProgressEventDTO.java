package com.project.fraudsystem.rag.dto;

public class RagProgressEventDTO {

    private String analysisId;
    private String type;
    private String step;
    private String status;
    private String message;
    private Long startedAtEpochMs;
    private Long elapsedMs;
    private RagResponseDTO result;

    public String getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStep() {
        return step;
    }

    public void setStep(String step) {
        this.step = step;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getStartedAtEpochMs() {
        return startedAtEpochMs;
    }

    public void setStartedAtEpochMs(Long startedAtEpochMs) {
        this.startedAtEpochMs = startedAtEpochMs;
    }

    public Long getElapsedMs() {
        return elapsedMs;
    }

    public void setElapsedMs(Long elapsedMs) {
        this.elapsedMs = elapsedMs;
    }

    public RagResponseDTO getResult() {
        return result;
    }

    public void setResult(RagResponseDTO result) {
        this.result = result;
    }
}
