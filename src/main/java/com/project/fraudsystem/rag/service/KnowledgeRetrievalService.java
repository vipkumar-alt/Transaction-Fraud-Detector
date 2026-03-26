package com.project.fraudsystem.rag.service;

import com.project.fraudsystem.rag.model.FraudKnowledgeChunk;
import com.project.fraudsystem.rag.repository.FraudKnowledgeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KnowledgeRetrievalService {

    private final FraudKnowledgeRepository repository;

    public KnowledgeRetrievalService(FraudKnowledgeRepository repository) {
        this.repository = repository;
    }

    public List<FraudKnowledgeChunk> getAllKnowledge() {
        return repository.findAll();
    }
}