package com.tracker.server.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class AdminRecordFilter {
    private String range = "today";

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;

    private String processName;
    private String windowName;
    private Long pid;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startTime;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endTime;

    private Long minDuration;
    private Long maxDuration;
    private String status;

    public boolean hasDataFilter() {
        return hasText(processName)
                || hasText(windowName)
                || pid != null
                || startTime != null
                || endTime != null
                || minDuration != null
                || maxDuration != null
                || hasText(status);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
