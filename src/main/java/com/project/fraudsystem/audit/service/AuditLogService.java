package com.project.fraudsystem.audit.service;

import com.project.fraudsystem.audit.model.RagAuditLog;
import com.project.fraudsystem.audit.repository.RagAuditLogRepository;
import com.project.fraudsystem.rag.dto.RagRequestDTO;
import com.project.fraudsystem.rag.dto.RagResponseDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.StringJoiner;

@Service
public class AuditLogService {

    private final RagAuditLogRepository repository;

    public AuditLogService(RagAuditLogRepository repository) {
        this.repository = repository;
    }

    public void logSuccess(
            RagRequestDTO request,
            RagResponseDTO response,
            Double fraudScore,
            LocalDateTime requestTime,
            LocalDateTime responseTime,
            long latencyMs
    ) {
        RagAuditLog log = new RagAuditLog();

        mapRequest(log, request);
        mapResponse(log, response);

        log.setFraudScore(fraudScore);
        log.setStatus("SUCCESS");
        log.setRequestReceivedAt(requestTime);
        log.setResponseGeneratedAt(responseTime);
        log.setLatencyMs(latencyMs);

        repository.save(log);
    }

    public void logFailure(
            RagRequestDTO request,
            Exception ex,
            Double fraudScore,
            LocalDateTime requestTime,
            LocalDateTime responseTime,
            long latencyMs
    ) {
        RagAuditLog log = new RagAuditLog();

        mapRequest(log, request);

        log.setFraudScore(fraudScore);
        log.setStatus("FAILED");
        log.setErrorMessage(ex.getMessage());
        log.setRequestReceivedAt(requestTime);
        log.setResponseGeneratedAt(responseTime);
        log.setLatencyMs(latencyMs);

        repository.save(log);
    }

    private void mapRequest(RagAuditLog log, RagRequestDTO request) {
        log.setAmount(request.getAmount());
        log.setMerchantCategory(request.getMerchantCategory());
        log.setDeviceType(request.getDeviceType());
        log.setNewDevice(request.isNewDevice());
        log.setInternational(request.isInternational());
        log.setTransactionTime(request.getTransactionTime());
        log.setAccountAgeDays(request.getAccountAgeDays());
        log.setTransactionsLast24h(request.getTransactionsLast24h());
    }

    private void mapResponse(RagAuditLog log, RagResponseDTO response) {
        log.setGeneratedQuery(response.getQuery());
        log.setRetrievedTitles(joinList(response.getRetrievedTitles()));
        log.setRetrievedContents(joinList(response.getRetrievedContents()));
        log.setExplanation(response.getExplanation());
        log.setRiskLevel(response.getRiskLevel());
        log.setRecommendedAction(response.getRecommendedAction());
    }

    private String joinList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }

        StringJoiner joiner = new StringJoiner(" | ");
        for (String value : values) {
            joiner.add(value);
        }
        return joiner.toString();
    }
}