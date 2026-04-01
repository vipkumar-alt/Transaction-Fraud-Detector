package com.project.fraudsystem.rag.service;

import com.project.fraudsystem.rag.dto.RagRequestDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class QueryBuilderService {

    public String buildQuery(RagRequestDTO request) {
        List<String> sentences = new ArrayList<>();

        sentences.add("Transaction amount is " + formatAmount(request.getAmount()) + ".");

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

        if (hasText(request.getTransactionTime())) {
            String timeSentence = buildTimeSentence(request.getTransactionTime());
            if (!timeSentence.isBlank()) {
                sentences.add(timeSentence);
            }
        }

        int accountAgeDays = request.getAccountAgeDays();
        sentences.add("Account age is " + accountAgeDays + " " + pluralize("day", accountAgeDays) + ".");

        int transactionsLast24h = request.getTransactionsLast24h();
        sentences.add(
                "The account made "
                        + transactionsLast24h
                        + " "
                        + pluralize("transaction", transactionsLast24h)
                        + " in the last 24 hours."
        );

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
