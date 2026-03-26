package com.project.fraudsystem.rag.controller;

import com.project.fraudsystem.rag.model.FraudKnowledgeChunk;
import com.project.fraudsystem.rag.service.KnowledgeRetrievalService;
import com.project.fraudsystem.rag.service.EmbeddingIngestionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rag/knowledge")
public class KnowledgeController {

    private final KnowledgeRetrievalService knowledgeRetrievalService;
    private final EmbeddingIngestionService embeddingIngestionService;

    public KnowledgeController(KnowledgeRetrievalService knowledgeRetrievalService,
                               EmbeddingIngestionService embeddingIngestionService) {
        this.knowledgeRetrievalService = knowledgeRetrievalService;
        this.embeddingIngestionService = embeddingIngestionService;
    }

    @GetMapping("/all")
    public List<FraudKnowledgeChunk> getAllKnowledge() {
        return knowledgeRetrievalService.getAllKnowledge();
    }

    @PostMapping("/embed")
    public String generateEmbeddings() {
        embeddingIngestionService.generateAndStoreEmbeddings();
        return "Embeddings generated successfully";
    }
}