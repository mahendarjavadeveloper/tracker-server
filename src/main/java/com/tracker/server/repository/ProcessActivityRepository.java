package com.tracker.server.repository;

import com.tracker.server.entity.ProcessActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProcessActivityRepository extends JpaRepository<ProcessActivity, Long> {
    Optional<ProcessActivity> findByLocalId(String localId);

    Optional<ProcessActivity> findFirstByDevice_IdAndPidAndProcessNameAndStartTimeAndEndTimeIsNull(
            Long deviceId, long pid, String processName, LocalDateTime startTime);

    List<ProcessActivity> findByDevice_IdOrderByStartTimeDesc(Long deviceId);

    List<ProcessActivity> findByDevice_IdAndStatus(Long deviceId, String status);
}
