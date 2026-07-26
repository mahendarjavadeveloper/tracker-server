package com.tracker.server.repository;

import com.tracker.server.entity.IdleActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IdleActivityRepository extends JpaRepository<IdleActivity, Long> {
    Optional<IdleActivity> findByLocalId(String localId);

    Optional<IdleActivity> findFirstByDevice_IdAndStartTimeAndEndTimeIsNull(Long deviceId, LocalDateTime startTime);

    List<IdleActivity> findByDevice_IdOrderByStartTimeDesc(Long deviceId);

    List<IdleActivity> findByDevice_IdAndStatus(Long deviceId, String status);
}
