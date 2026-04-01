package com.project.fraudsystem.rag.service;

import com.project.fraudsystem.rag.model.FraudKnowledgeChunk;
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

        List<FraudKnowledgeChunk> finalChunks = diversificationService.selectFinalChunks(
                List.of(primary, duplicatePrimary, guidance, contrast, extraContrast)
        );

        assertThat(finalChunks).extracting(FraudKnowledgeChunk::getId).containsExactly(1, 4, 3);
    }

    @Test
    void selectFinalChunksFallsBackToBestRemainingWhenBucketMissing() {
        FraudKnowledgeChunk primary = chunk(1, "New Device Takeover", "Device fraud signal", "device_pattern");
        FraudKnowledgeChunk guidance = chunk(2, "Manual Review Guidance", "Review this case", "review_guidance");
        FraudKnowledgeChunk extraPrimary = chunk(3, "Velocity Burst", "Fraud burst pattern", "fraud_pattern");

        List<FraudKnowledgeChunk> finalChunks = diversificationService.selectFinalChunks(
                List.of(primary, guidance, extraPrimary)
        );

        assertThat(finalChunks).extracting(FraudKnowledgeChunk::getId).containsExactly(1, 2, 3);
    }

    @Test
    void selectFinalChunksUsesKeywordFallbackWhenCategoryIsMissing() {
        FraudKnowledgeChunk primary = chunk(1, "Account Takeover Indicator", "Fraud signal", null);
        FraudKnowledgeChunk contrast = chunk(2, "Stable Consistent Pattern", "Normal baseline behavior", null);
        FraudKnowledgeChunk guidance = chunk(3, "OTP Verification Recommended", "Review guidance for the analyst", null);

        List<FraudKnowledgeChunk> finalChunks = diversificationService.selectFinalChunks(
                List.of(primary, contrast, guidance)
        );

        assertThat(finalChunks).extracting(FraudKnowledgeChunk::getId).containsExactly(1, 2, 3);
    }

    private FraudKnowledgeChunk chunk(int id, String title, String content, String category) {
        return new FraudKnowledgeChunk(id, title, content, category, "MEDIUM");
    }
}
