package com.project.fraudsystem.rag.service;

import com.project.fraudsystem.rag.dto.RagRequestDTO;
import com.project.fraudsystem.rag.dto.RagResponseDTO;
import com.project.fraudsystem.rag.model.FraudKnowledgeChunk;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagService {

    private final QueryBuilderService queryBuilderService;
    private final VectorSearchService vectorSearchService;

    public RagService(QueryBuilderService queryBuilderService,
                      VectorSearchService vectorSearchService) {
        this.queryBuilderService = queryBuilderService;
        this.vectorSearchService = vectorSearchService;
    }

    public RagResponseDTO buildOnlyQuery(RagRequestDTO request) {
        String query = queryBuilderService.buildQuery(request);

        List<FraudKnowledgeChunk> retrievedChunks = vectorSearchService.search(query);

        RagResponseDTO response = new RagResponseDTO();
        response.setQuery(query);
        response.setRetrievedContents(
                retrievedChunks.stream()
                        .map(FraudKnowledgeChunk::getContent)
                        .collect(Collectors.toList())
        );

        return response;
    }
}