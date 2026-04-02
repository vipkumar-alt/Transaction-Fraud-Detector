package com.project.fraudsystem.rag.service;

import com.project.fraudsystem.rag.dto.RagRequestDTO;
import com.project.fraudsystem.rag.dto.RagResponseDTO;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class RagAnalysisOrchestratorService {

    private final RagService ragService;
    private final RagProgressService ragProgressService;
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public RagAnalysisOrchestratorService(RagService ragService, RagProgressService ragProgressService) {
        this.ragService = ragService;
        this.ragProgressService = ragProgressService;
    }

    public String startAnalysis(RagRequestDTO request) {
        String analysisId = ragProgressService.createAnalysis();

        executorService.submit(() -> runAnalysis(analysisId, request));

        return analysisId;
    }

    private void runAnalysis(String analysisId, RagRequestDTO request) {
        try {
            RagResponseDTO response = ragService.buildOnlyQuery(
                    request,
                    (step, status, message) -> ragProgressService.publishProgress(analysisId, step, status, message)
            );
            ragProgressService.publishResult(analysisId, response);
        } catch (Exception ex) {
            ragProgressService.publishError(
                    analysisId,
                    "analysis",
                    ex.getMessage() != null ? ex.getMessage() : "Analysis failed."
            );
        }
    }
}
