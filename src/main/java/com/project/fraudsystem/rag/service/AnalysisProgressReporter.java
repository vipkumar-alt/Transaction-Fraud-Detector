package com.project.fraudsystem.rag.service;

@FunctionalInterface
public interface AnalysisProgressReporter {

    AnalysisProgressReporter NO_OP = (step, status, message) -> { };

    void onUpdate(String step, String status, String message);
}
