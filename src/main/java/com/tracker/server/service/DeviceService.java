package com.tracker.server.service;

import com.tracker.server.dto.DeviceDtos.HeartbeatRequest;
import com.tracker.server.dto.DeviceDtos.RegisterRequest;
import com.tracker.server.dto.DeviceDtos.ShutdownRequest;
import com.tracker.server.dto.DeviceDtos.UninstallRequest;
import com.tracker.server.entity.Device;
import com.tracker.server.entity.User;
import com.tracker.server.repository.DeviceRepository;
import com.tracker.server.repository.UserRepository;
import com.tracker.server.util.DateTimeUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

@Service
public class DeviceService {
    private static final Duration LOCATION_RETRY_DELAY = Duration.ofHours(6);

    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final IpGeolocationService ipGeolocationService;

    public DeviceService(
            DeviceRepository deviceRepository,
            UserRepository userRepository,
            IpGeolocationService ipGeolocationService
    ) {
        this.deviceRepository = deviceRepository;
        this.userRepository = userRepository;
        this.ipGeolocationService = ipGeolocationService;
    }

    @Transactional
    public Device register(Long userId, RegisterRequest request) {
        return register(userId, request, null);
    }

    @Transactional
    public Device register(Long userId, RegisterRequest request, String publicIpAddress) {
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
        device.setShutdownAt(null);
        device.setStatus("ONLINE");
        device.setLastSeen(DateTimeUtil.now());
        updatePublicIpLocation(device, publicIpAddress);
        return deviceRepository.save(device);
    }

    @Transactional
    public Device heartbeat(Long userId, Long deviceId, HeartbeatRequest request) {
        return heartbeat(userId, deviceId, request, null);
    }

    @Transactional
    public Device heartbeat(
            Long userId,
            Long deviceId,
            HeartbeatRequest request,
            String publicIpAddress
    ) {
        Device device = ownedForUpdate(userId, deviceId);
        if (device.isUninstalled()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Device is uninstalled");
        }
        device.setOnline(true);
        device.setStatus("ONLINE");
        device.setLastSeen(DateTimeUtil.now());
        if (request != null && request.ipAddress() != null && !request.ipAddress().isBlank()) {
            device.setLastIpAddress(request.ipAddress().trim());
        }
        updatePublicIpLocation(device, publicIpAddress);
        return device;
    }

    @Transactional
    public Device shutdown(Long userId, Long deviceId, ShutdownRequest request) {
        Device device = ownedForUpdate(userId, deviceId);
        LocalDateTime shutdownAt = request == null || request.shutdownAt() == null
                ? DateTimeUtil.now()
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
                ? DateTimeUtil.now()
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

    private void updatePublicIpLocation(Device device, String publicIpAddress) {
        String publicIp = clean(publicIpAddress);
        if (publicIp == null) {
            return;
        }

        boolean ipChanged = !Objects.equals(device.getPublicIpAddress(), publicIp);
        if (ipChanged) {
            device.setPublicIpAddress(publicIp);
            device.setCountry(null);
            device.setState(null);
            device.setIpLocationCheckedAt(null);
        }

        LocalDateTime checkedAt = device.getIpLocationCheckedAt();
        boolean locationMissing = device.getCountry() == null && device.getState() == null;
        boolean retryDue = checkedAt == null
                || checkedAt.plus(LOCATION_RETRY_DELAY).isBefore(DateTimeUtil.now());
        if (!ipChanged && (!locationMissing || !retryDue)) {
            return;
        }

        device.setIpLocationCheckedAt(DateTimeUtil.now());
        ipGeolocationService.find(publicIp).ifPresent(location -> {
            device.setCountry(location.country());
            device.setState(location.state());
        });
    }
}
