package com.project.fraudsystem.rag.controller;
import org.springframework.web.bind.annotation.CrossOrigin;

import com.project.fraudsystem.rag.dto.RagAnalysisStartResponseDTO;
import com.project.fraudsystem.rag.dto.RagRequestDTO;
import com.project.fraudsystem.rag.dto.RagResponseDTO;
import com.project.fraudsystem.rag.service.RagAnalysisOrchestratorService;
import com.project.fraudsystem.rag.service.RagProgressService;
import com.project.fraudsystem.rag.service.RagService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/rag")

@CrossOrigin(origins = "http://localhost:4200")
public class RagController {

    private final RagService ragService;
    private final RagAnalysisOrchestratorService ragAnalysisOrchestratorService;
    private final RagProgressService ragProgressService;

    public RagController(
            RagService ragService,
            RagAnalysisOrchestratorService ragAnalysisOrchestratorService,
            RagProgressService ragProgressService
    ) {
        this.ragService = ragService;
        this.ragAnalysisOrchestratorService = ragAnalysisOrchestratorService;
        this.ragProgressService = ragProgressService;
    }

    @PostMapping("/query")
    public RagResponseDTO generateQuery(@RequestBody RagRequestDTO request) {
        return ragService.buildOnlyQuery(request);
    }

    @PostMapping("/query/start")
    public RagAnalysisStartResponseDTO startQuery(@RequestBody RagRequestDTO request) {
        return new RagAnalysisStartResponseDTO(ragAnalysisOrchestratorService.startAnalysis(request));
    }

    @GetMapping(path = "/query/progress/{analysisId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamQueryProgress(@PathVariable String analysisId) {
        return ragProgressService.subscribe(analysisId);
    }
}
