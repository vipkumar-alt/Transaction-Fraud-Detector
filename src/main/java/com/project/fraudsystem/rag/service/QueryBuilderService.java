package com.project.fraudsystem.rag.service;

import com.project.fraudsystem.rag.dto.RagRequestDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class QueryBuilderService {

    public String buildQuery(RagRequestDTO request) {
        List<String> sentences = new ArrayList<>();

        sentences.add("Transaction amount is " + formatAmount(request.getAmount()) + ".");
        sentences.add(describeAmountBand(request.getAmount()));

        if (hasText(request.getMerchantCategory())) {
            sentences.add("Merchant category is " + normalizeValue(request.getMerchantCategory()) + ".");
        }

        if (hasText(request.getDeviceType())) {
            sentences.add("Device type is " + normalizeValue(request.getDeviceType()) + ".");
        }

        if (request.isNewDevice()) {
            sentences.add("The transaction is from a new device.");
        } else {
            sentences.add("The transaction is from a known device.");
        }

        if (request.isInternational()) {
            sentences.add("The transaction is international.");
        } else {
            sentences.add("The transaction is domestic, not international.");
        }

        String resolvedTransactionTime = resolveTransactionTime(request);
        if (hasText(resolvedTransactionTime)) {
            String timeSentence = buildTimeSentence(resolvedTransactionTime);
            if (!timeSentence.isBlank()) {
                sentences.add(timeSentence);
            }
        }

        int accountAgeDays = request.getAccountAgeDays();
        sentences.add("Account age is " + accountAgeDays + " " + pluralize("day", accountAgeDays) + ".");
        sentences.add(describeAccountAge(accountAgeDays));

        int transactionsLast24h = request.getTransactionsLast24h();
        sentences.add(
                "The account made "
                        + transactionsLast24h
                        + " "
                        + pluralize("transaction", transactionsLast24h)
                        + " in the last 24 hours."
        );
        sentences.add(describeVelocity(transactionsLast24h));

        return String.join(" ", sentences).trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String buildTimeSentence(String transactionTime) {
        String normalizedTime = transactionTime.trim();
        String timeOfDay = getTimeOfDayPhrase(normalizedTime);
        if (timeOfDay.isBlank()) {
            return "Transaction time is " + normalizedTime + ".";
        }
        return "Transaction time is " + normalizedTime + " " + timeOfDay + ".";
    }

    private String getTimeOfDayPhrase(String transactionTime) {
        try {
            LocalTime time = LocalTime.parse(transactionTime);

            if (time.isBefore(LocalTime.of(6, 0))) {
                return "at night";
            }
            if (time.isBefore(LocalTime.of(12, 0))) {
                return "in the morning";
            }
            if (time.isBefore(LocalTime.of(18, 0))) {
                return "in the afternoon";
            }
            return "in the evening";
        } catch (Exception e) {
            return "";
        }
    }

    private String resolveTransactionTime(RagRequestDTO request) {
        if (hasText(request.getTransactionTime())) {
            return request.getTransactionTime().trim();
        }
        if (!hasText(request.getTransactionTimestamp())) {
            return "";
        }

        String timestamp = request.getTransactionTimestamp().trim();
        try {
            return LocalDateTime.parse(timestamp).toLocalTime().toString();
        } catch (Exception ignored) {
            int separatorIndex = timestamp.indexOf('T');
            if (separatorIndex >= 0 && separatorIndex < timestamp.length() - 1) {
                return timestamp.substring(separatorIndex + 1).trim();
            }
            return timestamp;
        }
    }

    private String describeAmountBand(double amount) {
        if (amount <= 75.0) {
            return "This is a low-value transaction.";
        }
        if (amount <= 300.0) {
            return "This is a moderate-value transaction.";
        }
        return "This is a high-value transaction.";
    }

    private String describeAccountAge(int accountAgeDays) {
        if (accountAgeDays >= 365) {
            return "The account is well established.";
        }
        if (accountAgeDays >= 90) {
            return "The account has some established history.";
        }
        return "The account is relatively new.";
    }

    private String describeVelocity(int transactionsLast24h) {
        if (transactionsLast24h <= 2) {
            return "Recent transaction velocity is low.";
        }
        if (transactionsLast24h <= 5) {
            return "Recent transaction velocity is moderate.";
        }
        return "Recent transaction velocity is elevated.";
    }

    private String formatAmount(double amount) {
        return BigDecimal.valueOf(amount).stripTrailingZeros().toPlainString();
    }

    private String normalizeValue(String value) {
        return value.trim().toLowerCase();
    }

    private String pluralize(String singular, int count) {
        return count == 1 ? singular : singular + "s";
    }
}
