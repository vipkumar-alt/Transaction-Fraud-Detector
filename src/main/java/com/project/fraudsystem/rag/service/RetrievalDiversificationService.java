package com.project.fraudsystem.rag.service;

import com.project.fraudsystem.rag.model.FraudKnowledgeChunk;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class RetrievalDiversificationService {

    private static final int TARGET_FINAL_CHUNKS = 3;

    public List<FraudKnowledgeChunk> selectFinalChunks(List<FraudKnowledgeChunk> rerankedChunks) {
        List<FraudKnowledgeChunk> remaining = new ArrayList<>(rerankedChunks);
        List<FraudKnowledgeChunk> selected = new ArrayList<>();

        addFirstMatching(selected, remaining, KnowledgeBucket.PRIMARY);
        addFirstMatching(selected, remaining, KnowledgeBucket.CONTRAST);
        addFirstMatching(selected, remaining, KnowledgeBucket.GUIDANCE);

        while (selected.size() < TARGET_FINAL_CHUNKS && !remaining.isEmpty()) {
            selected.add(remaining.removeFirst());
        }

        return selected;
    }

    private void addFirstMatching(List<FraudKnowledgeChunk> selected,
                                  List<FraudKnowledgeChunk> remaining,
                                  KnowledgeBucket bucket) {
        for (int i = 0; i < remaining.size(); i++) {
            FraudKnowledgeChunk chunk = remaining.get(i);
            if (classify(chunk) == bucket) {
                selected.add(chunk);
                remaining.remove(i);
                return;
            }
        }
    }

    private KnowledgeBucket classify(FraudKnowledgeChunk chunk) {
        String category = normalize(chunk.getCategory());
        if (isPrimaryCategory(category)) {
            return KnowledgeBucket.PRIMARY;
        }
        if (isContrastCategory(category)) {
            return KnowledgeBucket.CONTRAST;
        }
        if ("review_guidance".equals(category)) {
            return KnowledgeBucket.GUIDANCE;
        }

        String text = normalize(chunk.getTitle()) + " " + normalize(chunk.getContent());
        if (containsAny(text, "review", "guidance", "allow", "block", "otp", "escalation")) {
            return KnowledgeBucket.GUIDANCE;
        }
        if (containsAny(text, "normal", "behavior", "baseline", "stable", "consistent")) {
            return KnowledgeBucket.CONTRAST;
        }
        return KnowledgeBucket.PRIMARY;
    }

    private boolean isPrimaryCategory(String category) {
        return "fraud_pattern".equals(category)
                || "device_pattern".equals(category)
                || "velocity_pattern".equals(category);
    }

    private boolean isContrastCategory(String category) {
        return "normal_pattern".equals(category)
                || "behavior_pattern".equals(category);
    }

    private boolean containsAny(String text, String... terms) {
        for (String term : terms) {
            if (text.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private enum KnowledgeBucket {
        PRIMARY,
        CONTRAST,
        GUIDANCE
    }
}
