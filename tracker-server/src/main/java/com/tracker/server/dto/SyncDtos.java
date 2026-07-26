package com.tracker.server.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SyncDtos {
    private SyncDtos() {
    }

    @Data
    public static class BatchRequest {
        @Valid
        private List<ProcessItem> processes = new ArrayList<>();

        @Valid
        private List<WindowItem> windows = new ArrayList<>();

        @Valid
        private List<IdleItem> idle = new ArrayList<>();

        @Valid
        private List<SessionItem> sessions = new ArrayList<>();
    }

    @Data
    public static class ProcessItem {
        @NotBlank
        private String localId;
        private long pid;
        @NotBlank
        private String processName;
        private String windowName;
        @NotNull
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private long durationSeconds;
        @NotBlank
        private String status;
    }

    @Data
    public static class WindowItem {
        @NotBlank
        private String localId;
        @NotBlank
        private String windowTitle;
        @NotNull
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private long durationSeconds;
        @NotBlank
        private String status;
    }

    @Data
    public static class IdleItem {
        @NotBlank
        private String localId;
        @NotNull
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private long durationSeconds;
        @NotBlank
        private String status;
    }

    @Data
    public static class SessionItem {
        @NotBlank
        private String localId;
        @NotNull
        private LocalDateTime startupTime;
        private LocalDateTime shutdownTime;
        private long durationSeconds;
        @NotBlank
        private String status;
    }

    @Data
    public static class BatchResponse {
        private Map<String, Long> processes = new LinkedHashMap<>();
        private Map<String, Long> windows = new LinkedHashMap<>();
        private Map<String, Long> idle = new LinkedHashMap<>();
        private Map<String, Long> sessions = new LinkedHashMap<>();
        private LocalDateTime acceptedAt = LocalDateTime.now();
    }
}
