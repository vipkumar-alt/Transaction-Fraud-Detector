package com.project.fraudsystem.rag.service;

import com.project.fraudsystem.rag.dto.RagProgressEventDTO;
import com.project.fraudsystem.rag.dto.RagResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class RagProgressService {

    private final Map<String, AnalysisChannel> channels = new ConcurrentHashMap<>();

    public String createAnalysis() {
        String analysisId = UUID.randomUUID().toString();
        channels.put(analysisId, new AnalysisChannel());
        publishProgress(analysisId, "accepted", "COMPLETED", "Analysis request accepted.");
        return analysisId;
    }

    public SseEmitter subscribe(String analysisId) {
        AnalysisChannel channel = channels.get(analysisId);
        if (channel == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown analysis id: " + analysisId);
        }

        SseEmitter emitter = new SseEmitter(0L);
        channel.emitter.set(emitter);

        emitter.onCompletion(() -> detachEmitter(analysisId, emitter));
        emitter.onTimeout(() -> {
            emitter.complete();
            detachEmitter(analysisId, emitter);
        });

        sendEvent(
                emitter,
                buildEvent(analysisId, "CONNECTED", "connected", "COMPLETED", "Progress stream connected.", null, null, null)
        );
        for (RagProgressEventDTO event : channel.history) {
            sendEvent(emitter, event);
        }

        if (channel.finished) {
            emitter.complete();
        }

        return emitter;
    }

    public void publishProgress(String analysisId, String step, String status, String message) {
        AnalysisChannel channel = channels.get(analysisId);
        if (channel == null) {
            return;
        }

        long now = System.currentTimeMillis();
        Long startedAtEpochMs = null;
        Long elapsedMs = null;

        if ("RUNNING".equalsIgnoreCase(status)) {
            startedAtEpochMs = now;
            channel.stepStartTimes.put(step, startedAtEpochMs);
        } else {
            startedAtEpochMs = channel.stepStartTimes.get(step);
            if (startedAtEpochMs != null) {
                elapsedMs = now - startedAtEpochMs;
            }
        }

        publishEvent(
                analysisId,
                buildEvent(analysisId, "PROGRESS", step, status, message, null, startedAtEpochMs, elapsedMs),
                false
        );
    }

    public void publishResult(String analysisId, RagResponseDTO response) {
        publishEvent(
                analysisId,
                buildEvent(
                        analysisId,
                        "RESULT",
                        "completed",
                        "COMPLETED",
                        "Analysis completed.",
                        response,
                        null,
                        response != null ? response.getLatencyMs() : null
                ),
                true
        );
    }

    public void publishError(String analysisId, String step, String message) {
        publishEvent(
                analysisId,
                buildEvent(analysisId, "ERROR", step, "FAILED", message, null, null, null),
                true
        );
    }

    private void publishEvent(String analysisId, RagProgressEventDTO event, boolean terminal) {
        AnalysisChannel channel = channels.get(analysisId);
        if (channel == null) {
            return;
        }

        channel.history.add(event);
        SseEmitter emitter = channel.emitter.get();
        if (emitter != null) {
            sendEvent(emitter, event);
        }

        if (terminal) {
            channel.finished = true;
            if (emitter != null) {
                emitter.complete();
            }
        }
    }

    private RagProgressEventDTO buildEvent(
            String analysisId,
            String type,
            String step,
            String status,
            String message,
            RagResponseDTO result,
            Long startedAtEpochMs,
            Long elapsedMs
    ) {
        RagProgressEventDTO event = new RagProgressEventDTO();
        event.setAnalysisId(analysisId);
        event.setType(type);
        event.setStep(step);
        event.setStatus(status);
        event.setMessage(message);
        event.setStartedAtEpochMs(startedAtEpochMs);
        event.setElapsedMs(elapsedMs);
        event.setResult(result);
        return event;
    }

    private void sendEvent(SseEmitter emitter, RagProgressEventDTO event) {
        try {
            emitter.send(SseEmitter.event().data(event, MediaType.APPLICATION_JSON));
        } catch (IOException ignored) {
            emitter.complete();
        }
    }

    private void detachEmitter(String analysisId, SseEmitter emitter) {
        AnalysisChannel channel = channels.get(analysisId);
        if (channel == null) {
            return;
        }

        channel.emitter.compareAndSet(emitter, null);
    }

    private static final class AnalysisChannel {
        private final List<RagProgressEventDTO> history = new CopyOnWriteArrayList<>();
        private final Map<String, Long> stepStartTimes = new ConcurrentHashMap<>();
        private final AtomicReference<SseEmitter> emitter = new AtomicReference<>();
        private volatile boolean finished;
    }
}
