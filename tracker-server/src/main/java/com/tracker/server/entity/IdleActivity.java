package com.tracker.server.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "idle_activity")
public class IdleActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "local_id", unique = true, length = 64)
    private String localId;

    @Column(name = "idle_start", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "idle_end")
    private LocalDateTime endTime;

    @Column(name = "idle_seconds", nullable = false)
    private long durationSeconds;

    @Column(nullable = false, length = 20)
    private String status;

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @JsonProperty("deviceId")
    public Long getDeviceId() {
        return device == null ? null : device.getId();
    }

    @JsonProperty("userId")
    public Long getUserId() {
        return user == null ? null : user.getId();
    }

    @JsonProperty("displayStatus")
    public String getDisplayStatus() {
        if ("RUNNING".equals(status) && device != null && !device.isOnline()) {
            return "SHUTDOWN".equals(device.getStatus()) ? "SHUTDOWN" : "OFFLINE";
        }
        return status;
    }

    @PrePersist
    void onCreate() {
        if (status == null || status.isBlank()) {
            status = "RUNNING";
        }
    }
}
