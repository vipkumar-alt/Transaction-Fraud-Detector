package com.project.fraudsystem.audit.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "rag_audit_log")
public class RagAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double amount;

    @Column(name = "merchant_category")
    private String merchantCategory;

    @Column(name = "device_type")
    private String deviceType;

    @Column(name = "new_device")
    private Boolean newDevice;

    @Column(name = "international")
    private Boolean international;

    @Column(name = "transaction_time")
    private String transactionTime;

    @Column(name = "account_age_days")
    private Integer accountAgeDays;

    @Column(name = "transactions_last_24h")
    private Integer transactionsLast24h;

    @Column(name = "fraud_score")
    private Double fraudScore;

    @Column(name = "generated_query", columnDefinition = "TEXT")
    private String generatedQuery;

    @Column(name = "retrieved_titles", columnDefinition = "TEXT")
    private String retrievedTitles;

    @Column(name = "retrieved_contents", columnDefinition = "TEXT")
    private String retrievedContents;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "risk_level")
    private String riskLevel;

    @Column(name = "recommended_action")
    private String recommendedAction;

    @Column(name = "status")
    private String status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "request_received_at")
    private LocalDateTime requestReceivedAt;

    @Column(name = "response_generated_at")
    private LocalDateTime responseGeneratedAt;

    @Column(name = "latency_ms")
    private Long latencyMs;

    public RagAuditLog() {
    }

    public Long getId() { return id; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getMerchantCategory() { return merchantCategory; }
    public void setMerchantCategory(String merchantCategory) { this.merchantCategory = merchantCategory; }

    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

    public Boolean getNewDevice() { return newDevice; }
    public void setNewDevice(Boolean newDevice) { this.newDevice = newDevice; }

    public Boolean getInternational() { return international; }
    public void setInternational(Boolean international) { this.international = international; }

    public String getTransactionTime() { return transactionTime; }
    public void setTransactionTime(String transactionTime) { this.transactionTime = transactionTime; }

    public Integer getAccountAgeDays() { return accountAgeDays; }
    public void setAccountAgeDays(Integer accountAgeDays) { this.accountAgeDays = accountAgeDays; }

    public Integer getTransactionsLast24h() { return transactionsLast24h; }
    public void setTransactionsLast24h(Integer transactionsLast24h) { this.transactionsLast24h = transactionsLast24h; }

    public Double getFraudScore() { return fraudScore; }
    public void setFraudScore(Double fraudScore) { this.fraudScore = fraudScore; }

    public String getGeneratedQuery() { return generatedQuery; }
    public void setGeneratedQuery(String generatedQuery) { this.generatedQuery = generatedQuery; }

    public String getRetrievedTitles() { return retrievedTitles; }
    public void setRetrievedTitles(String retrievedTitles) { this.retrievedTitles = retrievedTitles; }

    public String getRetrievedContents() { return retrievedContents; }
    public void setRetrievedContents(String retrievedContents) { this.retrievedContents = retrievedContents; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public String getRecommendedAction() { return recommendedAction; }
    public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getRequestReceivedAt() { return requestReceivedAt; }
    public void setRequestReceivedAt(LocalDateTime requestReceivedAt) { this.requestReceivedAt = requestReceivedAt; }

    public LocalDateTime getResponseGeneratedAt() { return responseGeneratedAt; }
    public void setResponseGeneratedAt(LocalDateTime responseGeneratedAt) { this.responseGeneratedAt = responseGeneratedAt; }

    public Long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Long latencyMs) { this.latencyMs = latencyMs; }
}