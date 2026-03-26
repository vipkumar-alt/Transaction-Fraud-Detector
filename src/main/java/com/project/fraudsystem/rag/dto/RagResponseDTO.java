package com.project.fraudsystem.rag.dto;

import java.util.List;

public class RagResponseDTO {

    private String query;
    private List<String> retrievedContents;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public List<String> getRetrievedContents() {
        return retrievedContents;
    }

    public void setRetrievedContents(List<String> retrievedContents) {
        this.retrievedContents = retrievedContents;
    }
}