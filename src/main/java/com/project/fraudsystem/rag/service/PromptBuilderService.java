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
                - Model Interpretation: %s

                Semantic Query:
                %s

                Retrieved Fraud Knowledge:
                %s

                Task:
                Analyze the transaction using BOTH the retrieved fraud knowledge and the machine learning fraud score.
                The machine learning fraud score is a strong signal and must influence the final explanation, risk level, and recommended action.
                If the retrieved knowledge and the ML score point in different directions, explain the conflict explicitly.
                Prefer LOW or MEDIUM outcomes when the ML score is below threshold or only slightly above threshold and the retrieved evidence is mixed.
                Use HIGH risk or BLOCK only when multiple independent risk indicators align strongly with a clearly elevated ML score and the retrieved context consistently supports fraud.
                If the evidence is mixed or only moderate, prefer MEDIUM risk with REVIEW instead of HIGH risk with BLOCK.
                Use retrieved low-risk or normal-pattern evidence when it matches the transaction details.
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
                resolveTransactionTime(request),
                request.getAccountAgeDays(),
                request.getTransactionsLast24h(),
                formatDouble(fraudModelScore.getFraudScore()),
                formatDouble(fraudModelScore.getFraudThreshold()),
                fraudModelScore.getModelDecision(),
                fraudModelScore.getModelName(),
                describeModelInterpretation(fraudModelScore),
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

    private String resolveTransactionTime(RagRequestDTO request) {
        if (request.getTransactionTime() != null && !request.getTransactionTime().isBlank()) {
            return request.getTransactionTime().trim();
        }
        if (request.getTransactionTimestamp() != null && !request.getTransactionTimestamp().isBlank()) {
            return request.getTransactionTimestamp().trim();
        }
        return "N/A";
    }

    private String describeModelInterpretation(FraudModelScore fraudModelScore) {
        if (fraudModelScore.getFraudScore() == null || fraudModelScore.getFraudThreshold() == null) {
            return "Model confidence is unavailable.";
        }

        double margin = fraudModelScore.getFraudScore() - fraudModelScore.getFraudThreshold();
        if (margin <= -0.08) {
            return "The model is comfortably below the fraud threshold and leans low risk.";
        }
        if (margin < 0.0) {
            return "The model is below the fraud threshold but still somewhat borderline.";
        }
        if (margin < 0.10) {
            return "The model is only slightly above the fraud threshold and should be treated as moderate or borderline risk.";
        }
        return "The model is well above the fraud threshold and supports strong fraud concern.";
    }
}
