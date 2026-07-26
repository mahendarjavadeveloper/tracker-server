package com.tracker.server.service;

import com.tracker.server.dto.DeviceDtos.HeartbeatRequest;
import com.tracker.server.dto.DeviceDtos.RegisterRequest;
import com.tracker.server.dto.DeviceDtos.ShutdownRequest;
import com.tracker.server.dto.DeviceDtos.UninstallRequest;
import com.tracker.server.entity.Device;
import com.tracker.server.entity.User;
import com.tracker.server.repository.DeviceRepository;
import com.tracker.server.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DeviceService {
    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;

    public DeviceService(DeviceRepository deviceRepository, UserRepository userRepository) {
        this.deviceRepository = deviceRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Device register(Long userId, RegisterRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        Device device = deviceRepository
                .findByInstallationIdAndUser_Id(request.installationId().trim(), userId)
                .orElseGet(Device::new);
        device.setInstallationId(request.installationId().trim());
        device.setMachineName(request.machineName().trim());
        device.setMacAddress(clean(request.macAddress()));
        device.setOsName(clean(request.osName()));
        device.setLastIpAddress(clean(request.ipAddress()));
        device.setUser(user);
        device.setOnline(true);
        device.setUninstalled(false);
        device.setUninstalledAt(null);
        device.setShutdownAt(null);
        device.setStatus("ONLINE");
        device.setLastSeen(LocalDateTime.now());
        return deviceRepository.save(device);
    }

    @Transactional
    public Device heartbeat(Long userId, Long deviceId, HeartbeatRequest request) {
        Device device = ownedForUpdate(userId, deviceId);
        if (device.isUninstalled()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Device is uninstalled");
        }
        device.setOnline(true);
        device.setStatus("ONLINE");
        device.setLastSeen(LocalDateTime.now());
        if (request != null && request.ipAddress() != null && !request.ipAddress().isBlank()) {
            device.setLastIpAddress(request.ipAddress().trim());
        }
        return device;
    }

    @Transactional
    public Device shutdown(Long userId, Long deviceId, ShutdownRequest request) {
        Device device = ownedForUpdate(userId, deviceId);
        LocalDateTime shutdownAt = request == null || request.shutdownAt() == null
                ? LocalDateTime.now()
                : request.shutdownAt();
        device.setOnline(false);
        device.setStatus("SHUTDOWN");
        device.setShutdownAt(shutdownAt);
        device.setLastSeen(shutdownAt);
        return device;
    }

    @Transactional
    public Device uninstall(Long userId, Long deviceId, UninstallRequest request) {
        Device device = ownedForUpdate(userId, deviceId);
        LocalDateTime uninstalledAt = request == null || request.uninstalledAt() == null
                ? LocalDateTime.now()
                : request.uninstalledAt();
        device.setUninstalled(true);
        device.setUninstalledAt(uninstalledAt);
        device.setOnline(false);
        device.setStatus("UNINSTALLED");
        device.setLastSeen(uninstalledAt);
        return device;
    }

    @Transactional(readOnly = true)
    public List<Device> list(Long userId) {
        return deviceRepository.findByUser_IdOrderByIdDesc(userId);
    }

    private Device ownedForUpdate(Long userId, Long deviceId) {
        Device device = deviceRepository.findByIdForUpdate(deviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));
        if (!device.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Device does not belong to user");
        }
        return device;
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
