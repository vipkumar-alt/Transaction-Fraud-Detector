package com.project.fraudsystem.rag.service;

import com.project.fraudsystem.rag.dto.RagRequestDTO;
import com.project.fraudsystem.rag.model.FraudKnowledgeChunk;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PromptBuilderService {

    public String buildPrompt(RagRequestDTO request, String query, List<FraudKnowledgeChunk> chunks) {

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

                Semantic Query:
                %s

                Retrieved Fraud Knowledge:
                %s

                Task:
                Analyze the transaction using ONLY the retrieved fraud knowledge.
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
                query,
                retrievedContext
        );
    }
}