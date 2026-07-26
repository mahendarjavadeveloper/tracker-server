package com.tracker.server.repository;

import com.tracker.server.entity.Device;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {
    Optional<Device> findByInstallationIdAndUser_Id(String installationId, Long userId);

    Optional<Device> findByIdAndUser_Id(Long id, Long userId);

    List<Device> findByUser_IdOrderByIdDesc(Long userId);

    List<Device> findAllByOrderByIdDesc();

    long countByStatus(String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Device d where d.id = :id")
    Optional<Device> findByIdForUpdate(@Param("id") Long id);

    List<Device> findByOnlineTrueAndUninstalledFalseAndLastSeenBefore(LocalDateTime threshold);
}
