package com.tracker.server.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tracker.server.util.DateTimeUtil;
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
@Table(name = "devices")
public class Device {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "installation_id", unique = true, length = 64)
    private String installationId;

    @Column(name = "mac_address", length = 64)
    private String macAddress;

    @Column(name = "machine_name", nullable = false, length = 255)
    private String machineName;

    @Column(name = "os_name", length = 255)
    private String osName;

    @Column(name = "last_ip_address", length = 64)
    private String lastIpAddress;

    @Column(name = "public_ip_address", length = 64)
    private String publicIpAddress;

    @Column(name = "country_name", length = 100)
    private String country;

    @Column(name = "state_name", length = 150)
    private String state;

    @JsonIgnore
    @Column(name = "ip_location_checked_at")
    private LocalDateTime ipLocationCheckedAt;

    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private boolean online;

    @Column(nullable = false)
    private boolean uninstalled;

    @Column(name = "shutdown_at")
    private LocalDateTime shutdownAt;

    @Column(name = "uninstalled_at")
    private LocalDateTime uninstalledAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @JsonProperty("userId")
    public Long getUserId() {
        return user == null ? null : user.getId();
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = DateTimeUtil.now();
        }
        if (status == null || status.isBlank()) {
            status = "OFFLINE";
        }
    }
}
