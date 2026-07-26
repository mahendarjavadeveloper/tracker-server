package com.tracker.server.repository;

import com.tracker.server.entity.ActiveWindowActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ActiveWindowActivityRepository extends JpaRepository<ActiveWindowActivity, Long> {
    Optional<ActiveWindowActivity> findByLocalId(String localId);

    Optional<ActiveWindowActivity> findFirstByDevice_IdAndWindowTitleAndStartTimeAndEndTimeIsNull(
            Long deviceId, String windowTitle, LocalDateTime startTime);

    List<ActiveWindowActivity> findByDevice_IdOrderByStartTimeDesc(Long deviceId);

    List<ActiveWindowActivity> findByDevice_IdAndStatus(Long deviceId, String status);
}
