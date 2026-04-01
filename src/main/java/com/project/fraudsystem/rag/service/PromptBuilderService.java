package com.project.fraudsystem.rag.service;

import com.project.fraudsystem.rag.dto.RagRequestDTO;
import com.project.fraudsystem.rag.model.FraudModelScore;
import com.project.fraudsystem.rag.model.FraudKnowledgeChunk;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class PromptBuilderService {

    public String buildPrompt(RagRequestDTO request, String query, List<FraudKnowledgeChunk> chunks, FraudModelScore fraudModelScore) {

        String retrievedContext = chunks.stream()
                .map(chunk -> """
                        Title: %s
                        Category: %s
                        Risk Level: %s
                        Content: %s
                        """.formatted(
                        chunk.getTitle(),
                        chunk.getCategory(),
                        chunk.getRiskLevel(),
                        chunk.getContent()
                ))
                .collect(Collectors.joining("\n\n"));

        if (retrievedContext.isBlank()) {
            retrievedContext = "No fraud knowledge was retrieved.";
        }

        return """
                You are a fraud analysis assistant.

                Transaction Details:
                - Amount: %s
                - Merchant Category: %s
                - Device Type: %s
                - New Device: %s
                - International: %s
                - Transaction Time: %s
                - Account Age Days: %s
                - Transactions Last 24h: %s

                Machine Learning Fraud Assessment:
                - Fraud Score: %s
                - Fraud Threshold: %s
                - Model Decision: %s
                - Model Name: %s

                Semantic Query:
                %s

                Retrieved Fraud Knowledge:
                %s

                Task:
                Analyze the transaction using BOTH the retrieved fraud knowledge and the machine learning fraud score.
                The machine learning fraud score is a strong signal and must influence the final explanation, risk level, and recommended action.
                If the retrieved knowledge and the ML score point in different directions, explain the conflict explicitly and choose the safer final action.
                Return:
                1. A short explanation
                2. A risk level: LOW, MEDIUM, or HIGH
                3. A recommended action: ALLOW, REVIEW, or BLOCK

                Format your answer exactly like this:
                Explanation: <text>
                RiskLevel: <LOW|MEDIUM|HIGH>
                RecommendedAction: <ALLOW|REVIEW|BLOCK>
                """.formatted(
                request.getAmount(),
                request.getMerchantCategory(),
                request.getDeviceType(),
                request.isNewDevice(),
                request.isInternational(),
                request.getTransactionTime(),
                request.getAccountAgeDays(),
                request.getTransactionsLast24h(),
                formatDouble(fraudModelScore.getFraudScore()),
                formatDouble(fraudModelScore.getFraudThreshold()),
                fraudModelScore.getModelDecision(),
                fraudModelScore.getModelName(),
                query,
                retrievedContext
        );
    }

    private String formatDouble(Double value) {
        if (value == null) {
            return "N/A";
        }
        return String.format(Locale.ROOT, "%.4f", value);
    }
}
