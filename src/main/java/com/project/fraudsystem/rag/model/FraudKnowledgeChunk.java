package com.project.fraudsystem.rag.model;

public class FraudKnowledgeChunk {

    private Integer id;
    private String title;
    private String content;
    private String category;
    private String riskLevel;

    public FraudKnowledgeChunk() {
    }

    public FraudKnowledgeChunk(Integer id, String title, String content, String category, String riskLevel) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.category = category;
        this.riskLevel = riskLevel;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }
}