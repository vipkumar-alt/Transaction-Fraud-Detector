package com.project.fraudsystem.rag.service;

import com.project.fraudsystem.rag.dto.RagRequestDTO;
import com.project.fraudsystem.rag.model.FraudKnowledgeChunk;
import com.project.fraudsystem.rag.model.FraudModelScore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RetrievalDiversificationServiceTest {

    private final RetrievalDiversificationService diversificationService = new RetrievalDiversificationService();

    @Test
    void selectFinalChunksPrefersPrimaryContrastAndGuidanceMix() {
        FraudKnowledgeChunk primary = chunk(1, "New Device Takeover", "Device fraud signal", "device_pattern");
        FraudKnowledgeChunk duplicatePrimary = chunk(2, "Velocity Burst", "Fraud burst pattern", "fraud_pattern");
        FraudKnowledgeChunk guidance = chunk(3, "Manual Review Guidance", "Review this case", "review_guidance");
        FraudKnowledgeChunk contrast = chunk(4, "Misc Net Moderate Amount Pattern", "Normal contrast pattern", "normal_pattern");
        FraudKnowledgeChunk extraContrast = chunk(5, "Stable Repeat Behavior", "Behavior baseline", "behavior_pattern");
        RagRequestDTO request = riskyRequest();
        FraudModelScore fraudModelScore = fraudModelScore(0.91, 0.55);

        List<FraudKnowledgeChunk> finalChunks = diversificationService.selectFinalChunks(
                request,
                fraudModelScore,
                List.of(primary, duplicatePrimary, guidance, contrast, extraContrast)
        );

        assertThat(finalChunks).extracting(FraudKnowledgeChunk::getId).containsExactly(1, 3, 4);
    }

    @Test
    void selectFinalChunksFallsBackToBestRemainingWhenBucketMissing() {
        FraudKnowledgeChunk primary = chunk(1, "New Device Takeover", "Device fraud signal", "device_pattern");
        FraudKnowledgeChunk guidance = chunk(2, "Manual Review Guidance", "Review this case", "review_guidance");
        FraudKnowledgeChunk extraPrimary = chunk(3, "Velocity Burst", "Fraud burst pattern", "fraud_pattern");
        RagRequestDTO request = riskyRequest();
        FraudModelScore fraudModelScore = fraudModelScore(0.84, 0.55);

        List<FraudKnowledgeChunk> finalChunks = diversificationService.selectFinalChunks(
                request,
                fraudModelScore,
                List.of(primary, guidance, extraPrimary)
        );

        assertThat(finalChunks).extracting(FraudKnowledgeChunk::getId).containsExactly(1, 2, 3);
    }

    @Test
    void selectFinalChunksUsesKeywordFallbackWhenCategoryIsMissing() {
        FraudKnowledgeChunk primary = chunk(1, "Account Takeover Indicator", "Fraud signal", null);
        FraudKnowledgeChunk contrast = chunk(2, "Stable Consistent Pattern", "Normal baseline behavior", null);
        FraudKnowledgeChunk guidance = chunk(3, "OTP Verification Recommended", "Review guidance for the analyst", null);
        RagRequestDTO request = riskyRequest();
        FraudModelScore fraudModelScore = fraudModelScore(0.82, 0.55);

        List<FraudKnowledgeChunk> finalChunks = diversificationService.selectFinalChunks(
                request,
                fraudModelScore,
                List.of(primary, contrast, guidance)
        );

        assertThat(finalChunks).extracting(FraudKnowledgeChunk::getId).containsExactly(1, 3, 2);
    }

    private FraudKnowledgeChunk chunk(int id, String title, String content, String category) {
        return new FraudKnowledgeChunk(id, title, content, category, "MEDIUM");
    }

    private RagRequestDTO riskyRequest() {
        RagRequestDTO request = new RagRequestDTO();
        request.setAmount(850.0);
        request.setNewDevice(true);
        request.setInternational(true);
        request.setTransactionsLast24h(7);
        request.setAccountAgeDays(10);
        return request;
    }

    private FraudModelScore fraudModelScore(double score, double threshold) {
        FraudModelScore fraudModelScore = new FraudModelScore();
        fraudModelScore.setFraudScore(score);
        fraudModelScore.setFraudThreshold(threshold);
        return fraudModelScore;
    }
}
