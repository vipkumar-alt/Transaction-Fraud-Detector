package com.project.fraudsystem.rag.service;

import com.project.fraudsystem.rag.dto.RagRequestDTO;
import com.project.fraudsystem.rag.model.FraudKnowledgeChunk;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class RetrievalConsistencyFilterService {

    private static final double INTERNATIONAL_CONFLICT_PENALTY = 0.30;
    private static final double DEVICE_CONFLICT_PENALTY = 0.25;
    private static final double TIME_CONFLICT_PENALTY = 0.18;
    private static final double LOW_AMOUNT_CONFLICT_PENALTY = 0.24;
    private static final double LOW_VELOCITY_CONFLICT_PENALTY = 0.26;
    private static final double ACCOUNT_AGE_CONFLICT_PENALTY = 0.20;
    private static final double EXTREMELY_STRONG_DISTANCE = 0.12;

    private static final String[] INTERNATIONAL_TERMS = {
            "international", "cross-border", "cross border", "foreign", "overseas", "abroad"
    };
    private static final String[] DOMESTIC_TERMS = {
            "domestic", "local transaction", "in-country", "in country", "same country"
    };
    private static final String[] NEW_DEVICE_TERMS = {
            "new device", "unknown device", "unrecognized device", "first-time device", "first time device"
    };
    private static final String[] KNOWN_DEVICE_TERMS = {
            "known device", "trusted device", "recognized device", "existing device"
    };
    private static final String[] NIGHT_TERMS = {
            "late-night", "late night", "night-time", "nighttime", "at night", "overnight"
    };
    private static final String[] HIGH_AMOUNT_TERMS = {
            "high amount", "high-value", "high value", "large transaction", "large payment",
            "amount spike", "extreme value", "luxury", "outlier"
    };
    private static final String[] HIGH_VELOCITY_TERMS = {
            "high velocity", "rapid", "burst", "many transactions", "repeated", "card testing",
            "dense cluster", "high frequency", "multiple transactions"
    };
    private static final String[] YOUNG_ACCOUNT_TERMS = {
            "low account age", "young account", "new account", "first transaction after dormancy", "account takeover"
    };

    public List<FraudKnowledgeChunk> rerank(RagRequestDTO request, List<FraudKnowledgeChunk> chunks) {
        return chunks.stream()
                .sorted(Comparator.comparingDouble(chunk -> adjustedDistance(request, chunk)))
                .toList();
    }

    double adjustedDistance(RagRequestDTO request, FraudKnowledgeChunk chunk) {
        return baseDistance(chunk) + calculatePenalty(request, chunk);
    }

    private double calculatePenalty(RagRequestDTO request, FraudKnowledgeChunk chunk) {
        String searchableText = searchableText(chunk);
        double penalty = 0.0;

        if (request.isInternational()) {
            if (containsAny(searchableText, DOMESTIC_TERMS)) {
                penalty += INTERNATIONAL_CONFLICT_PENALTY;
            }
        } else if (containsAny(searchableText, INTERNATIONAL_TERMS)) {
            penalty += INTERNATIONAL_CONFLICT_PENALTY;
        }

        if (request.isNewDevice()) {
            if (containsAny(searchableText, KNOWN_DEVICE_TERMS)) {
                penalty += DEVICE_CONFLICT_PENALTY;
            }
        } else if (containsAny(searchableText, NEW_DEVICE_TERMS)) {
            penalty += DEVICE_CONFLICT_PENALTY;
        }

        if (isKnownNonNightTransaction(request)
                && containsAny(searchableText, NIGHT_TERMS)
                && baseDistance(chunk) > EXTREMELY_STRONG_DISTANCE) {
            penalty += TIME_CONFLICT_PENALTY;
        }

        if (request.getAmount() <= 75.0
                && containsAny(searchableText, HIGH_AMOUNT_TERMS)
                && baseDistance(chunk) > EXTREMELY_STRONG_DISTANCE) {
            penalty += LOW_AMOUNT_CONFLICT_PENALTY;
        }

        if (request.getTransactionsLast24h() <= 2
                && containsAny(searchableText, HIGH_VELOCITY_TERMS)
                && baseDistance(chunk) > EXTREMELY_STRONG_DISTANCE) {
            penalty += LOW_VELOCITY_CONFLICT_PENALTY;
        }

        if (request.getAccountAgeDays() >= 365
                && containsAny(searchableText, YOUNG_ACCOUNT_TERMS)
                && baseDistance(chunk) > EXTREMELY_STRONG_DISTANCE) {
            penalty += ACCOUNT_AGE_CONFLICT_PENALTY;
        }

        return penalty;
    }

    private double baseDistance(FraudKnowledgeChunk chunk) {
        return chunk.getDistance() != null ? chunk.getDistance() : Double.MAX_VALUE;
    }

    private String searchableText(FraudKnowledgeChunk chunk) {
        String title = chunk.getTitle() == null ? "" : chunk.getTitle();
        String content = chunk.getContent() == null ? "" : chunk.getContent();
        return (title + " " + content).toLowerCase(Locale.ROOT);
    }

    private boolean containsAny(String text, String[] terms) {
        for (String term : terms) {
            Pattern pattern = Pattern.compile("(^|[^a-z])" + Pattern.quote(term) + "([^a-z]|$)");
            if (pattern.matcher(text).find()) {
                return true;
            }
        }
        return false;
    }

    private boolean isKnownNonNightTransaction(RagRequestDTO request) {
        String resolvedTime = resolveTransactionTime(request);
        if (resolvedTime.isBlank()) {
            return false;
        }

        try {
            return !LocalTime.parse(resolvedTime).isBefore(LocalTime.of(6, 0));
        } catch (Exception e) {
            return false;
        }
    }

    private String resolveTransactionTime(RagRequestDTO request) {
        if (request.getTransactionTime() != null && !request.getTransactionTime().isBlank()) {
            return normalizeTimeValue(request.getTransactionTime());
        }
        return normalizeTimeValue(request.getTransactionTimestamp());
    }

    private String normalizeTimeValue(String transactionTime) {
        if (transactionTime == null || transactionTime.isBlank()) {
            return "";
        }
        String normalized = transactionTime.trim();
        try {
            return LocalTime.parse(normalized).toString();
        } catch (Exception ignored) {
            try {
                return LocalDateTime.parse(normalized).toLocalTime().toString();
            } catch (Exception ignoredAgain) {
                int separatorIndex = normalized.indexOf('T');
                if (separatorIndex >= 0 && separatorIndex < normalized.length() - 1) {
                    return normalized.substring(separatorIndex + 1).trim();
                }
                return "";
            }
        }
    }
}
