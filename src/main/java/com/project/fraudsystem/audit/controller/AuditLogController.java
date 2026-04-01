package com.project.fraudsystem.audit.controller;
import org.springframework.web.bind.annotation.CrossOrigin;

import com.project.fraudsystem.audit.model.RagAuditLog;
import com.project.fraudsystem.audit.repository.RagAuditLogRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
@CrossOrigin(origins = "http://localhost:4200")
public class AuditLogController {

    private final RagAuditLogRepository repository;

    public AuditLogController(RagAuditLogRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/all")
    public List<RagAuditLog> getAllLogs() {
        return repository.findAll();
    }
}