//package com.project.fraudsystem.rag.service;
//
//import com.project.fraudsystem.rag.dto.RagRequestDTO;
//import com.project.fraudsystem.rag.dto.RagResponseDTO;
//import com.project.fraudsystem.rag.model.FraudKnowledgeChunk;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Service
//public class RagService {
//
//    private final QueryBuilderService queryBuilderService;
//    private final VectorSearchService vectorSearchService;
//    private final PromptBuilderService promptBuilderService;
//    private final LlmExplanationService llmExplanationService;
//    private final LlmResponseParserService llmResponseParserService;
//
//    public RagService(QueryBuilderService queryBuilderService,
//                      VectorSearchService vectorSearchService,
//                      PromptBuilderService promptBuilderService,
//                      LlmExplanationService llmExplanationService,
//                      LlmResponseParserService llmResponseParserService) {
//        this.queryBuilderService = queryBuilderService;
//        this.vectorSearchService = vectorSearchService;
//        this.promptBuilderService = promptBuilderService;
//        this.llmExplanationService = llmExplanationService;
//        this.llmResponseParserService = llmResponseParserService;
//    }
//
//    public RagResponseDTO buildOnlyQuery(RagRequestDTO request) {
//        String query = queryBuilderService.buildQuery(request);
//
//        List<FraudKnowledgeChunk> retrievedChunks = vectorSearchService.search(query);
//
//        RagResponseDTO response = new RagResponseDTO();
//        response.setQuery(query);
//        response.setRetrievedTitles(
//                retrievedChunks.stream()
//                        .map(FraudKnowledgeChunk::getTitle)
//                        .collect(Collectors.toList())
//        );
//        response.setRetrievedContents(
//                retrievedChunks.stream()
//                        .map(FraudKnowledgeChunk::getContent)
//                        .collect(Collectors.toList())
//        );
//
//        String prompt = promptBuilderService.buildPrompt(request, query, retrievedChunks);
//        String llmResponse = llmExplanationService.generateResponse(prompt);
//        llmResponseParserService.parseAndApply(llmResponse, response);
//
//        return response;
//    }
//}


package com.project.fraudsystem.rag.service;

import com.project.fraudsystem.audit.service.AuditLogService;
import com.project.fraudsystem.rag.dto.RagRequestDTO;
import com.project.fraudsystem.rag.dto.RagResponseDTO;
import com.project.fraudsystem.rag.model.FraudKnowledgeChunk;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagService {

    private final QueryBuilderService queryBuilderService;
    private final VectorSearchService vectorSearchService;
    private final PromptBuilderService promptBuilderService;
    private final LlmExplanationService llmExplanationService;
    private final LlmResponseParserService llmResponseParserService;
    private final AuditLogService auditLogService;

    public RagService(QueryBuilderService queryBuilderService,
                      VectorSearchService vectorSearchService,
                      PromptBuilderService promptBuilderService,
                      LlmExplanationService llmExplanationService,
                      LlmResponseParserService llmResponseParserService,
                      AuditLogService auditLogService) {
        this.queryBuilderService = queryBuilderService;
        this.vectorSearchService = vectorSearchService;
        this.promptBuilderService = promptBuilderService;
        this.llmExplanationService = llmExplanationService;
        this.llmResponseParserService = llmResponseParserService;
        this.auditLogService = auditLogService;
    }

    public RagResponseDTO buildOnlyQuery(RagRequestDTO request) {
        LocalDateTime requestTime = LocalDateTime.now();
        long startMillis = System.currentTimeMillis();
        Double fraudScore = null; // future ML integration

        try {
            String query = queryBuilderService.buildQuery(request);

            List<FraudKnowledgeChunk> retrievedChunks = vectorSearchService.search(query);

            RagResponseDTO response = new RagResponseDTO();
            response.setQuery(query);
            response.setRetrievedTitles(
                    retrievedChunks.stream()
                            .map(FraudKnowledgeChunk::getTitle)
                            .collect(Collectors.toList())
            );
            response.setRetrievedContents(
                    retrievedChunks.stream()
                            .map(FraudKnowledgeChunk::getContent)
                            .collect(Collectors.toList())
            );

            String prompt = promptBuilderService.buildPrompt(request, query, retrievedChunks);
            String llmResponse = llmExplanationService.generateResponse(prompt);
            llmResponseParserService.parseAndApply(llmResponse, response);

            LocalDateTime responseTime = LocalDateTime.now();
            long latencyMs = System.currentTimeMillis() - startMillis;

            auditLogService.logSuccess(
                    request,
                    response,
                    fraudScore,
                    requestTime,
                    responseTime,
                    latencyMs
            );

            return response;

        } catch (Exception ex) {
            LocalDateTime responseTime = LocalDateTime.now();
            long latencyMs = System.currentTimeMillis() - startMillis;

            auditLogService.logFailure(
                    request,
                    ex,
                    fraudScore,
                    requestTime,
                    responseTime,
                    latencyMs
            );

            throw ex;
        }
    }
}