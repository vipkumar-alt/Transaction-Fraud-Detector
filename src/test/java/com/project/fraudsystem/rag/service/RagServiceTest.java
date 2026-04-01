package com.project.fraudsystem.rag.service;

import com.project.fraudsystem.rag.dto.RagRequestDTO;
import com.project.fraudsystem.rag.dto.RagResponseDTO;
import com.project.fraudsystem.rag.model.FraudModelScore;
import com.project.fraudsystem.rag.model.FraudKnowledgeChunk;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RagServiceTest {

    @Test
    void buildOnlyQueryAddsFraudScoreToResponseAndPromptFlow() {
        QueryBuilderService queryBuilderService = mock(QueryBuilderService.class);
        FraudModelScoreService fraudModelScoreService = mock(FraudModelScoreService.class);
        VectorSearchService vectorSearchService = mock(VectorSearchService.class);
        RetrievalConsistencyFilterService retrievalConsistencyFilterService = mock(RetrievalConsistencyFilterService.class);
        RetrievalDiversificationService retrievalDiversificationService = mock(RetrievalDiversificationService.class);
        PromptBuilderService promptBuilderService = mock(PromptBuilderService.class);
        LlmExplanationService llmExplanationService = mock(LlmExplanationService.class);
        LlmResponseParserService llmResponseParserService = mock(LlmResponseParserService.class);

        RagService ragService = new RagService(
                queryBuilderService,
                fraudModelScoreService,
                vectorSearchService,
                retrievalConsistencyFilterService,
                retrievalDiversificationService,
                promptBuilderService,
                llmExplanationService,
                llmResponseParserService
        );

        RagRequestDTO request = new RagRequestDTO();
        request.setAmount(4500);

        FraudModelScore fraudModelScore = new FraudModelScore();
        fraudModelScore.setFraudScore(0.88);
        fraudModelScore.setFraudThreshold(0.41);
        fraudModelScore.setModelDecision("FRAUD");
        fraudModelScore.setModelName("lightgbm");

        List<FraudKnowledgeChunk> chunks = List.of(
                new FraudKnowledgeChunk(1, "Chunk 1", "content 1", "category", "HIGH")
        );

        when(queryBuilderService.buildQuery(request)).thenReturn("high value");
        when(fraudModelScoreService.score(request)).thenReturn(fraudModelScore);
        when(vectorSearchService.search("high value")).thenReturn(chunks);
        when(retrievalConsistencyFilterService.rerank(request, chunks)).thenReturn(chunks);
        when(retrievalDiversificationService.selectFinalChunks(chunks)).thenReturn(chunks);
        when(promptBuilderService.buildPrompt(request, "high value", chunks, fraudModelScore)).thenReturn("prompt");
        when(llmExplanationService.generateResponse("prompt")).thenReturn("""
                Explanation: suspicious transaction
                RiskLevel: HIGH
                RecommendedAction: BLOCK
                """);
        doAnswer(invocation -> {
            String llmResponse = invocation.getArgument(0);
            RagResponseDTO response = invocation.getArgument(1);
            response.setExplanation("suspicious transaction");
            response.setRiskLevel("HIGH");
            response.setRecommendedAction("BLOCK");
            assertThat(llmResponse).contains("Explanation:");
            return null;
        }).when(llmResponseParserService).parseAndApply(any(), any());

        RagResponseDTO response = ragService.buildOnlyQuery(request);

        assertThat(response.getFraudScore()).isEqualTo(0.88);
        assertThat(response.getFraudThreshold()).isEqualTo(0.41);
        assertThat(response.getFraudModelName()).isEqualTo("lightgbm");
        assertThat(response.getFraudModelDecision()).isEqualTo("FRAUD");
        assertThat(response.getRetrievedTitles()).containsExactly("Chunk 1");
        assertThat(response.getRetrievedContents()).containsExactly("content 1");
        assertThat(response.getExplanation()).isEqualTo("suspicious transaction");
        assertThat(response.getRiskLevel()).isEqualTo("HIGH");
        assertThat(response.getRecommendedAction()).isEqualTo("BLOCK");

        verify(promptBuilderService).buildPrompt(request, "high value", chunks, fraudModelScore);
        verify(llmResponseParserService).parseAndApply(eq("""
                Explanation: suspicious transaction
                RiskLevel: HIGH
                RecommendedAction: BLOCK
                """), any(RagResponseDTO.class));
    }
}
