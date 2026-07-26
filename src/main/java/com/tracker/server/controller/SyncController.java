package com.tracker.server.controller;

import com.tracker.server.dto.SyncDtos.BatchRequest;
import com.tracker.server.dto.SyncDtos.BatchResponse;
import com.tracker.server.security.TrackerPrincipal;
import com.tracker.server.service.TrackerSyncService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sync")
public class SyncController {
    private final TrackerSyncService trackerSyncService;

    public SyncController(TrackerSyncService trackerSyncService) {
        this.trackerSyncService = trackerSyncService;
    }

    @PostMapping("/{deviceId}")
    public BatchResponse sync(
            @AuthenticationPrincipal TrackerPrincipal principal,
            @PathVariable Long deviceId,
            @Valid @RequestBody BatchRequest request
    ) {
        return trackerSyncService.sync(principal.userId(), deviceId, request);
    }
}
