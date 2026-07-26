package com.tracker.server.repository;

import com.tracker.server.entity.DeviceSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DeviceSessionRepository extends JpaRepository<DeviceSession, Long> {
    Optional<DeviceSession> findByLocalId(String localId);

    Optional<DeviceSession> findFirstByDevice_IdAndStartupTimeAndShutdownTimeIsNull(Long deviceId, LocalDateTime startupTime);

    List<DeviceSession> findByDevice_IdOrderByStartupTimeDesc(Long deviceId);

    List<DeviceSession> findByDevice_IdAndStatus(Long deviceId, String status);
}
