package com.project.fraudsystem.rag.service;

import com.project.fraudsystem.rag.dto.RagRequestDTO;
import com.project.fraudsystem.rag.model.FraudModelScore;
import com.project.fraudsystem.rag.model.FraudKnowledgeChunk;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PromptBuilderServiceTest {

    private final PromptBuilderService promptBuilderService = new PromptBuilderService();

    @Test
    void buildPromptIncludesFraudScoreGuidance() {
        RagRequestDTO request = new RagRequestDTO();
        request.setAmount(4500);
        request.setMerchantCategory("crypto exchange");
        request.setDeviceType("mobile");
        request.setNewDevice(true);
        request.setInternational(true);
        request.setTransactionTime("02:15");
        request.setAccountAgeDays(15);
        request.setTransactionsLast24h(8);

        FraudModelScore fraudModelScore = new FraudModelScore();
        fraudModelScore.setFraudScore(0.91);
        fraudModelScore.setFraudThreshold(0.43);
        fraudModelScore.setModelDecision("FRAUD");
        fraudModelScore.setModelName("xgboost");

        String prompt = promptBuilderService.buildPrompt(
                request,
                "high value crypto exchange",
                List.of(new FraudKnowledgeChunk(1, "Crypto risk", "High-risk merchant behavior", "merchant", "HIGH")),
                fraudModelScore
        );

        assertThat(prompt).contains("Machine Learning Fraud Assessment");
        assertThat(prompt).contains("Fraud Score: 0.9100");
        assertThat(prompt).contains("Model Decision: FRAUD");
        assertThat(prompt).contains("must influence the final explanation, risk level, and recommended action");
    }
}
