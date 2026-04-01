package com.project.fraudsystem.rag.service;

import com.project.fraudsystem.rag.dto.RagRequestDTO;
import com.project.fraudsystem.rag.dto.RagResponseDTO;
import com.project.fraudsystem.rag.model.FraudModelScore;
import com.project.fraudsystem.rag.model.FraudKnowledgeChunk;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagService {

    private final QueryBuilderService queryBuilderService;
    private final FraudModelScoreService fraudModelScoreService;
    private final VectorSearchService vectorSearchService;
    private final RetrievalConsistencyFilterService retrievalConsistencyFilterService;
    private final RetrievalDiversificationService retrievalDiversificationService;
    private final PromptBuilderService promptBuilderService;
    private final LlmExplanationService llmExplanationService;
    private final LlmResponseParserService llmResponseParserService;

    public RagService(QueryBuilderService queryBuilderService,
                      FraudModelScoreService fraudModelScoreService,
                      VectorSearchService vectorSearchService,
                      RetrievalConsistencyFilterService retrievalConsistencyFilterService,
                      RetrievalDiversificationService retrievalDiversificationService,
                      PromptBuilderService promptBuilderService,
                      LlmExplanationService llmExplanationService,
                      LlmResponseParserService llmResponseParserService) {
        this.queryBuilderService = queryBuilderService;
        this.fraudModelScoreService = fraudModelScoreService;
        this.vectorSearchService = vectorSearchService;
        this.retrievalConsistencyFilterService = retrievalConsistencyFilterService;
        this.retrievalDiversificationService = retrievalDiversificationService;
        this.promptBuilderService = promptBuilderService;
        this.llmExplanationService = llmExplanationService;
        this.llmResponseParserService = llmResponseParserService;
    }

    public RagResponseDTO buildOnlyQuery(RagRequestDTO request) {
        String query = queryBuilderService.buildQuery(request);
        FraudModelScore fraudModelScore = fraudModelScoreService.score(request);

        List<FraudKnowledgeChunk> retrievedChunks =
                retrievalConsistencyFilterService.rerank(request, vectorSearchService.search(query));
        List<FraudKnowledgeChunk> finalChunks = retrievalDiversificationService.selectFinalChunks(retrievedChunks);
        for (FraudKnowledgeChunk chunk : finalChunks) {
            System.out.println(
                    "TITLE: " + chunk.getTitle()
                            + " | CATEGORY: " + chunk.getCategory()
                            + " | RISK: " + chunk.getRiskLevel()
                            + " | DISTANCE: " + chunk.getDistance()
            );
        }

        RagResponseDTO response = new RagResponseDTO();
        response.setQuery(query);
        response.setFraudScore(fraudModelScore.getFraudScore());
        response.setFraudThreshold(fraudModelScore.getFraudThreshold());
        response.setFraudModelName(fraudModelScore.getModelName());
        response.setFraudModelDecision(fraudModelScore.getModelDecision());
        response.setRetrievedTitles(
                finalChunks.stream()
                        .map(FraudKnowledgeChunk::getTitle)
                        .collect(Collectors.toList())
        );
        response.setRetrievedContents(
                finalChunks.stream()
                        .map(FraudKnowledgeChunk::getContent)
                        .collect(Collectors.toList())
        );

        String prompt = promptBuilderService.buildPrompt(request, query, finalChunks, fraudModelScore);
        String llmResponse = llmExplanationService.generateResponse(prompt);
        llmResponseParserService.parseAndApply(llmResponse, response);

        return response;
    }
}
