package com.project.fraudsystem.rag.service;

import com.project.fraudsystem.rag.dto.RagRequestDTO;
import org.springframework.stereotype.Service;

@Service
public class QueryBuilderService {

    public String buildQuery(RagRequestDTO request) {
        StringBuilder query = new StringBuilder();

        if (request.getAmount() > 3000) {
            query.append("high value ");
        }

        if (request.isInternational()) {
            query.append("international ");
        }

        if (request.isNewDevice()) {
            query.append("new device ");
        }

        if (request.getMerchantCategory() != null) {
            query.append(request.getMerchantCategory()).append(" ");
        }

        return query.toString().trim();
    }
}