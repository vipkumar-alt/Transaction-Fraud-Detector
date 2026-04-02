package com.project.fraudsystem.rag.service;

import com.project.fraudsystem.rag.dto.RagRequestDTO;
import com.project.fraudsystem.rag.model.FraudKnowledgeChunk;
import com.project.fraudsystem.rag.model.FraudModelScore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class RetrievalDiversificationService {

    private static final int TARGET_FINAL_CHUNKS = 3;

    public List<FraudKnowledgeChunk> selectFinalChunks(RagRequestDTO request,
                                                       FraudModelScore fraudModelScore,
                                                       List<FraudKnowledgeChunk> rerankedChunks) {
        List<FraudKnowledgeChunk> remaining = new ArrayList<>(rerankedChunks);
        List<FraudKnowledgeChunk> selected = new ArrayList<>();

        for (KnowledgeBucket bucket : preferredBucketOrder(request, fraudModelScore)) {
            addFirstMatching(selected, remaining, bucket);
        }

        while (selected.size() < TARGET_FINAL_CHUNKS && !remaining.isEmpty()) {
            selected.add(remaining.removeFirst());
        }

        return selected;
    }

    private List<KnowledgeBucket> preferredBucketOrder(RagRequestDTO request, FraudModelScore fraudModelScore) {
        if (isClearlyLowRisk(request, fraudModelScore)) {
            return List.of(KnowledgeBucket.CONTRAST, KnowledgeBucket.GUIDANCE, KnowledgeBucket.PRIMARY);
        }
        if (isBorderlineRisk(fraudModelScore)) {
            return List.of(KnowledgeBucket.CONTRAST, KnowledgeBucket.PRIMARY, KnowledgeBucket.GUIDANCE);
        }
        return List.of(KnowledgeBucket.PRIMARY, KnowledgeBucket.GUIDANCE, KnowledgeBucket.CONTRAST);
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
        String riskLevel = normalize(chunk.getRiskLevel());
        String text = normalize(chunk.getTitle()) + " " + normalize(chunk.getContent());

        if ("review_guidance".equals(category)) {
            return KnowledgeBucket.GUIDANCE;
        }
        if ("low".equals(riskLevel) || isContrastCategory(category)) {
            return KnowledgeBucket.CONTRAST;
        }
        if (isPrimaryCategory(category)) {
            return KnowledgeBucket.PRIMARY;
        }
        if (containsAny(text, "review", "guidance", "allow", "block", "otp", "escalation")) {
            return KnowledgeBucket.GUIDANCE;
        }
        if (containsAny(text, "normal", "behavior", "baseline", "stable", "consistent")) {
            return KnowledgeBucket.CONTRAST;
        }
        return KnowledgeBucket.PRIMARY;
    }

    private boolean isClearlyLowRisk(RagRequestDTO request, FraudModelScore fraudModelScore) {
        if (fraudModelScore == null || fraudModelScore.getFraudScore() == null || fraudModelScore.getFraudThreshold() == null) {
            return false;
        }
        boolean stableRequest = !request.isNewDevice()
                && !request.isInternational()
                && request.getTransactionsLast24h() <= 2
                && request.getAmount() <= 150.0
                && request.getAccountAgeDays() >= 180;
        return stableRequest && fraudModelScore.getFraudScore() < (fraudModelScore.getFraudThreshold() - 0.05);
    }

    private boolean isBorderlineRisk(FraudModelScore fraudModelScore) {
        if (fraudModelScore == null || fraudModelScore.getFraudScore() == null || fraudModelScore.getFraudThreshold() == null) {
            return false;
        }
        double score = fraudModelScore.getFraudScore();
        double threshold = fraudModelScore.getFraudThreshold();
        return score < threshold + 0.10;
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
