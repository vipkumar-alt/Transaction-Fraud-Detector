package com.project.fraudsystem.rag.service;

import com.project.fraudsystem.rag.dto.RagResponseDTO;
import org.springframework.stereotype.Service;

@Service
public class LlmResponseParserService {

    public void parseAndApply(String llmResponse, RagResponseDTO dto) {
        String[] lines = llmResponse.split("\\r?\\n");

        for (String line : lines) {
            if (line.startsWith("Explanation:")) {
                dto.setExplanation(line.replace("Explanation:", "").trim());
            } else if (line.startsWith("RiskLevel:")) {
                dto.setRiskLevel(line.replace("RiskLevel:", "").trim());
            } else if (line.startsWith("RecommendedAction:")) {
                dto.setRecommendedAction(line.replace("RecommendedAction:", "").trim());
            }
        }
    }
}