package com.project.fraudsystem.rag.service;

import com.project.fraudsystem.audit.service.AuditLogService;
import com.project.fraudsystem.rag.dto.RagRequestDTO;
import com.project.fraudsystem.rag.dto.RagResponseDTO;
import com.project.fraudsystem.rag.model.FraudKnowledgeChunk;
import com.project.fraudsystem.rag.model.FraudModelScore;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagService {

    private final QueryBuilderService queryBuilderService;
    private final FraudModelScoreService fraudModelScoreService;
    private final VectorSearchService vectorSearchService;
    private final RetrievalConsistencyFilterService retrievalConsistencyFilterService;
    private final RetrievalDiversificationService retrievalDiversificationService;
    private final PromptBuilderService promptBuilderService;
    private final LlmExplanationService llmExplanationService;
    private final LlmResponseParserService llmResponseParserService;
    private final AuditLogService auditLogService;

    public RagService(QueryBuilderService queryBuilderService,
                      FraudModelScoreService fraudModelScoreService,
                      VectorSearchService vectorSearchService,
                      RetrievalConsistencyFilterService retrievalConsistencyFilterService,
                      RetrievalDiversificationService retrievalDiversificationService,
                      PromptBuilderService promptBuilderService,
                      LlmExplanationService llmExplanationService,
                      LlmResponseParserService llmResponseParserService,
                      AuditLogService auditLogService) {
        this.queryBuilderService = queryBuilderService;
        this.fraudModelScoreService = fraudModelScoreService;
        this.vectorSearchService = vectorSearchService;
        this.retrievalConsistencyFilterService = retrievalConsistencyFilterService;
        this.retrievalDiversificationService = retrievalDiversificationService;
        this.promptBuilderService = promptBuilderService;
        this.llmExplanationService = llmExplanationService;
        this.llmResponseParserService = llmResponseParserService;
        this.auditLogService = auditLogService;
    }

    public RagResponseDTO buildOnlyQuery(RagRequestDTO request) {
        return buildOnlyQuery(request, AnalysisProgressReporter.NO_OP);
    }

    public RagResponseDTO buildOnlyQuery(RagRequestDTO request, AnalysisProgressReporter progressReporter) {
        LocalDateTime requestTime = LocalDateTime.now();
        long startMillis = System.currentTimeMillis();
        Double fraudScore = null;

        try {
            progressReporter.onUpdate("query_building", "RUNNING", "Building semantic search query.");
            String query = queryBuilderService.buildQuery(request);
            progressReporter.onUpdate("query_building", "COMPLETED", "Query prepared for retrieval.");

            progressReporter.onUpdate("ml_scoring", "RUNNING", "Scoring transaction with fraud model.");
            FraudModelScore fraudModelScore = fraudModelScoreService.score(request);
            fraudScore = fraudModelScore.getFraudScore();
            progressReporter.onUpdate(
                    "ml_scoring",
                    "COMPLETED",
                    "ML scoring completed with " + fraudModelScore.getModelName() + "."
            );

            progressReporter.onUpdate("rag_retrieval", "RUNNING", "Generating embedding and retrieving relevant knowledge.");
            List<FraudKnowledgeChunk> retrievedChunks =
                    retrievalConsistencyFilterService.rerank(request, vectorSearchService.search(query));
            List<FraudKnowledgeChunk> finalChunks = retrievalDiversificationService.selectFinalChunks(
                    request,
                    fraudModelScore,
                    retrievedChunks
            );
            for (FraudKnowledgeChunk chunk : finalChunks) {
                System.out.println(
                        "TITLE: " + chunk.getTitle()
                                + " | CATEGORY: " + chunk.getCategory()
                                + " | RISK: " + chunk.getRiskLevel()
                                + " | DISTANCE: " + chunk.getDistance()
                );
            }
            progressReporter.onUpdate(
                    "rag_retrieval",
                    "COMPLETED",
                    "Retrieved " + finalChunks.size() + " supporting knowledge chunks."
            );

            RagResponseDTO response = new RagResponseDTO();
            response.setQuery(query);
            response.setFraudScore(fraudModelScore.getFraudScore());
            response.setFraudThreshold(fraudModelScore.getFraudThreshold());
            response.setFraudModelName(fraudModelScore.getModelName());
            response.setFraudModelDecision(fraudModelScore.getModelDecision());
            response.setRetrievedTitles(
                    finalChunks.stream()
                            .map(FraudKnowledgeChunk::getTitle)
                            .collect(Collectors.toList())
            );
            response.setRetrievedContents(
                    finalChunks.stream()
                            .map(FraudKnowledgeChunk::getContent)
                            .collect(Collectors.toList())
            );

            progressReporter.onUpdate("llm_explanation", "RUNNING", "Generating explanation from retrieved evidence.");
            String prompt = promptBuilderService.buildPrompt(request, query, finalChunks, fraudModelScore);
            String llmResponse = llmExplanationService.generateResponse(prompt);
            llmResponseParserService.parseAndApply(llmResponse, response);
            calibrateDecision(request, finalChunks, fraudModelScore, response);
            progressReporter.onUpdate("llm_explanation", "COMPLETED", "Explanation and recommendation are ready.");

            LocalDateTime responseTime = LocalDateTime.now();
            long latencyMs = System.currentTimeMillis() - startMillis;
            response.setLatencyMs(latencyMs);

            progressReporter.onUpdate("audit_logging", "RUNNING", "Writing audit trail.");
            auditLogService.logSuccess(
                    request,
                    response,
                    fraudScore,
                    requestTime,
                    responseTime,
                    latencyMs
            );
            progressReporter.onUpdate("audit_logging", "COMPLETED", "Audit log saved.");

            return response;
        } catch (Exception ex) {
            LocalDateTime responseTime = LocalDateTime.now();
            long latencyMs = System.currentTimeMillis() - startMillis;

            progressReporter.onUpdate("audit_logging", "RUNNING", "Writing failure audit trail.");

            auditLogService.logFailure(
                    request,
                    ex,
                    fraudScore,
                    requestTime,
                    responseTime,
                    latencyMs
            );
            progressReporter.onUpdate("analysis", "FAILED", "Analysis failed.");

            throw ex;
        }
    }

    private void calibrateDecision(RagRequestDTO request,
                                   List<FraudKnowledgeChunk> finalChunks,
                                   FraudModelScore fraudModelScore,
                                   RagResponseDTO response) {
        if (fraudModelScore == null || fraudModelScore.getFraudScore() == null || fraudModelScore.getFraudThreshold() == null) {
            return;
        }

        double score = fraudModelScore.getFraudScore();
        double threshold = fraudModelScore.getFraudThreshold();
        long lowRiskChunkCount = finalChunks.stream()
                .filter(chunk -> "low".equalsIgnoreCase(chunk.getRiskLevel()) || "normal_pattern".equalsIgnoreCase(chunk.getCategory()))
                .count();
        long highRiskChunkCount = finalChunks.stream()
                .filter(chunk -> "high".equalsIgnoreCase(chunk.getRiskLevel()))
                .count();
        long nonHighChunkCount = finalChunks.stream()
                .filter(chunk -> !"high".equalsIgnoreCase(chunk.getRiskLevel()))
                .count();

        boolean stableRequest = !request.isNewDevice()
                && !request.isInternational()
                && request.getTransactionsLast24h() <= 2
                && request.getAmount() <= 150.0
                && request.getAccountAgeDays() >= 180;
        boolean moderateMixedContext = !request.isInternational()
                && request.getAmount() <= 300.0
                && request.getTransactionsLast24h() <= 5
                && request.getAccountAgeDays() >= 90;
        boolean clearlyLowMlRisk = score < (threshold - 0.05);
        boolean borderlineMlRisk = score < (threshold + 0.10);
        boolean changed = false;

        if (borderlineMlRisk && "BLOCK".equalsIgnoreCase(response.getRecommendedAction())) {
            response.setRecommendedAction("REVIEW");
            response.setRiskLevel("MEDIUM");
            changed = true;
        }

        if (clearlyLowMlRisk && lowRiskChunkCount >= highRiskChunkCount) {
            if ("HIGH".equalsIgnoreCase(response.getRiskLevel())) {
                response.setRiskLevel("MEDIUM");
                changed = true;
            }
            if ("BLOCK".equalsIgnoreCase(response.getRecommendedAction())) {
                response.setRecommendedAction("REVIEW");
                changed = true;
            }
        }

        if (clearlyLowMlRisk && stableRequest && lowRiskChunkCount >= 1 && highRiskChunkCount <= 1) {
            response.setRiskLevel("LOW");
            response.setRecommendedAction("ALLOW");
            changed = true;
        }

        if (score < 0.90
                && moderateMixedContext
                && nonHighChunkCount >= 2
                && highRiskChunkCount <= 1
                && ("HIGH".equalsIgnoreCase(response.getRiskLevel()) || "BLOCK".equalsIgnoreCase(response.getRecommendedAction()))) {
            response.setRiskLevel("MEDIUM");
            response.setRecommendedAction("REVIEW");
            changed = true;
        }

        if (changed) {
            String explanation = response.getExplanation() == null ? "" : response.getExplanation().trim();
            String calibrationNote = " Final decision was calibrated toward a lower-severity outcome because the ML score was not strongly elevated and the retrieved evidence was mixed or low risk.";
            if (!explanation.endsWith(calibrationNote.trim())) {
                response.setExplanation((explanation + calibrationNote).trim());
            }
        }
    }
}
