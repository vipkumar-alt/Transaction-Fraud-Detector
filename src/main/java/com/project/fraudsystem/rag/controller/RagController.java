package com.project.fraudsystem.rag.controller;
import org.springframework.web.bind.annotation.CrossOrigin;

import com.project.fraudsystem.rag.dto.RagRequestDTO;
import com.project.fraudsystem.rag.dto.RagResponseDTO;
import com.project.fraudsystem.rag.service.RagService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rag")

@CrossOrigin(origins = "http://localhost:4200")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/query")
    public RagResponseDTO generateQuery(@RequestBody RagRequestDTO request) {
        return ragService.buildOnlyQuery(request);
    }
}