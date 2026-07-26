package com.tracker.server.scheduler;

import com.tracker.server.entity.Device;
import com.tracker.server.repository.DeviceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class DeviceStatusScheduler {
    private final DeviceRepository deviceRepository;
    private final long offlineAfterSeconds;

    public DeviceStatusScheduler(
            DeviceRepository deviceRepository,
            @Value("${app.device.offline-after-seconds}") long offlineAfterSeconds
    ) {
        this.deviceRepository = deviceRepository;
        this.offlineAfterSeconds = offlineAfterSeconds;
    }

    @Scheduled(fixedDelayString = "${app.device.offline-check-ms:10000}")
    @Transactional
    public void markMissingHeartbeatsOffline() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(offlineAfterSeconds);
        for (Device device : deviceRepository
                .findByOnlineTrueAndUninstalledFalseAndLastSeenBefore(threshold)) {
            device.setOnline(false);
            device.setStatus("OFFLINE");
        }
    }
}
