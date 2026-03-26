package com.project.fraudsystem.rag.service;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    public EmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public List<Double> generateEmbedding(String text) {
        float[] embedding = embeddingModel.embed(text);

        List<Double> vector = new ArrayList<>(embedding.length);
        for (float value : embedding) {
            vector.add((double) value);
        }
        return vector;
    }
}