package com.project.fraudsystem.rag.service;

import com.project.fraudsystem.rag.model.FraudKnowledgeChunk;
import com.project.fraudsystem.rag.repository.FraudKnowledgeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VectorSearchService {

    private final FraudKnowledgeRepository repository;
    private final EmbeddingService embeddingService;

    public VectorSearchService(FraudKnowledgeRepository repository,
                               EmbeddingService embeddingService) {
        this.repository = repository;
        this.embeddingService = embeddingService;
    }

    public List<FraudKnowledgeChunk> search(String query) {
        List<Double> queryEmbedding = embeddingService.generateEmbedding(query);
        String queryVector = queryEmbedding.toString().replace(" ", "");
        return repository.searchByVector(queryVector);
    }
}