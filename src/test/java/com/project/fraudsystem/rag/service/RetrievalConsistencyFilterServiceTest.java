package com.project.fraudsystem.rag.service;

import com.project.fraudsystem.rag.dto.RagRequestDTO;
import com.project.fraudsystem.rag.model.FraudKnowledgeChunk;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RetrievalConsistencyFilterServiceTest {

    private final RetrievalConsistencyFilterService filterService = new RetrievalConsistencyFilterService();

    @Test
    void rerankPenalizesInternationalChunksForDomesticRequest() {
        RagRequestDTO request = new RagRequestDTO();
        request.setInternational(false);

        FraudKnowledgeChunk internationalChunk = chunk(
                1,
                "Cross-border card testing",
                "International fraud pattern for foreign merchants",
                0.08
        );
        FraudKnowledgeChunk domesticChunk = chunk(
                2,
                "Domestic card velocity",
                "Domestic routine transaction behavior",
                0.18
        );

        List<FraudKnowledgeChunk> reranked = filterService.rerank(request, List.of(internationalChunk, domesticChunk));

        assertThat(reranked).extracting(FraudKnowledgeChunk::getId).containsExactly(2, 1);
    }

    @Test
    void rerankPenalizesKnownDeviceChunksForNewDeviceRequest() {
        RagRequestDTO request = new RagRequestDTO();
        request.setNewDevice(true);

        FraudKnowledgeChunk knownDeviceChunk = chunk(
                1,
                "Trusted device behavior",
                "Low-risk spending from a known device profile",
                0.09
        );
        FraudKnowledgeChunk newDeviceChunk = chunk(
                2,
                "New device takeover",
                "Unrecognized device activity with account takeover risk",
                0.20
        );

        List<FraudKnowledgeChunk> reranked = filterService.rerank(request, List.of(knownDeviceChunk, newDeviceChunk));

        assertThat(reranked).extracting(FraudKnowledgeChunk::getId).containsExactly(2, 1);
    }

    @Test
    void rerankPenalizesNightChunksForDaytimeRequestWhenDistanceIsNotExtremelyStrong() {
        RagRequestDTO request = new RagRequestDTO();
        request.setTransactionTime("14:30");

        FraudKnowledgeChunk nightChunk = chunk(
                1,
                "Late-night spending burst",
                "Night-time fraud patterns are common here",
                0.14
        );
        FraudKnowledgeChunk daytimeChunk = chunk(
                2,
                "Afternoon merchant review",
                "Daytime purchase behavior",
                0.20
        );

        List<FraudKnowledgeChunk> reranked = filterService.rerank(request, List.of(nightChunk, daytimeChunk));

        assertThat(reranked).extracting(FraudKnowledgeChunk::getId).containsExactly(2, 1);
    }

    @Test
    void rerankKeepsNightChunkWhenDistanceIsExtremelyStrong() {
        RagRequestDTO request = new RagRequestDTO();
        request.setTransactionTime("14:30");

        FraudKnowledgeChunk nightChunk = chunk(
                1,
                "Late-night spending burst",
                "Night-time fraud patterns are common here",
                0.05
        );
        FraudKnowledgeChunk daytimeChunk = chunk(
                2,
                "Afternoon merchant review",
                "Daytime purchase behavior",
                0.20
        );

        List<FraudKnowledgeChunk> reranked = filterService.rerank(request, List.of(nightChunk, daytimeChunk));

        assertThat(reranked).extracting(FraudKnowledgeChunk::getId).containsExactly(1, 2);
    }

    private FraudKnowledgeChunk chunk(int id, String title, String content, double distance) {
        FraudKnowledgeChunk chunk = new FraudKnowledgeChunk(id, title, content, "category", "HIGH");
        chunk.setDistance(distance);
        return chunk;
    }
}
