package com.project.fraudsystem.rag.service;

import com.project.fraudsystem.rag.model.FraudKnowledgeChunk;
import com.project.fraudsystem.rag.repository.FraudKnowledgeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmbeddingIngestionService {

    private final FraudKnowledgeRepository repository;
    private final EmbeddingService embeddingService;

    public EmbeddingIngestionService(FraudKnowledgeRepository repository,
                                     EmbeddingService embeddingService) {
        this.repository = repository;
        this.embeddingService = embeddingService;
    }

    public void generateAndStoreEmbeddings() {

        List<FraudKnowledgeChunk> chunks = repository.findAll();

        for (FraudKnowledgeChunk chunk : chunks) {

            List<Double> embedding = embeddingService.generateEmbedding(chunk.getContent());

            repository.updateEmbedding(chunk.getId(), embedding);
        }
    }
}