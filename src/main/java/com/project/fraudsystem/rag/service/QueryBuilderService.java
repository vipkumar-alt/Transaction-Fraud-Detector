package com.project.fraudsystem.rag.service;

import com.project.fraudsystem.rag.dto.RagRequestDTO;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class QueryBuilderService {

    public String buildQuery(RagRequestDTO request) {
        List<String> parts = new ArrayList<>();

        // Amount-based signals
        if (request.getAmount() >= 3000) {
            parts.add("high value");
        } else if (request.getAmount() > 0 && request.getAmount() <= 100) {
            parts.add("low value");
        } else if (request.getAmount() == 0) {
            parts.add("zero amount");
        }

        // Merchant category
        if (hasText(request.getMerchantCategory())) {
            parts.add(request.getMerchantCategory().trim().toLowerCase());
        }

        // Device signals
        if (hasText(request.getDeviceType())) {
            parts.add(request.getDeviceType().trim().toLowerCase());
        }

        if (request.isNewDevice()) {
            parts.add("new device");
        } else {
            parts.add("known device");
        }

        // Geography / international
        if (request.isInternational()) {
            parts.add("international");
        } else {
            parts.add("domestic");
        }

        // Time-based signal
        if (hasText(request.getTransactionTime())) {
            String timeSignal = getTimeSignal(request.getTransactionTime());
            if (!timeSignal.isBlank()) {
                parts.add(timeSignal);
            }
        }

        // Account age
        if (request.getAccountAgeDays() <= 30) {
            parts.add("new account");
        } else if (request.getAccountAgeDays() >= 365) {
            parts.add("old account");
        }

        // Velocity
        if (request.getTransactionsLast24h() >= 5) {
            parts.add("high frequency");
        } else if (request.getTransactionsLast24h() <= 1) {
            parts.add("low frequency");
        }

        return String.join(" ", parts).trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String getTimeSignal(String transactionTime) {
        try {
            LocalTime time = LocalTime.parse(transactionTime);

            if (time.isAfter(LocalTime.of(0, 0)) && time.isBefore(LocalTime.of(6, 0))) {
                return "late night";
            } else if (time.isAfter(LocalTime.of(6, 0)) && time.isBefore(LocalTime.of(12, 0))) {
                return "morning";
            } else if (time.isAfter(LocalTime.of(12, 0)) && time.isBefore(LocalTime.of(18, 0))) {
                return "afternoon";
            } else {
                return "evening";
            }
        } catch (Exception e) {
            return "";
        }
    }
}