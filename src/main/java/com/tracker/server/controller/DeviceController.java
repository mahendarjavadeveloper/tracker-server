package com.tracker.server.controller;

import com.tracker.server.dto.DeviceDtos.HeartbeatRequest;
import com.tracker.server.dto.DeviceDtos.RegisterRequest;
import com.tracker.server.dto.DeviceDtos.ShutdownRequest;
import com.tracker.server.dto.DeviceDtos.UninstallRequest;
import com.tracker.server.entity.Device;
import com.tracker.server.security.TrackerPrincipal;
import com.tracker.server.service.ClientIpResolver;
import com.tracker.server.service.DeviceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {
    private final DeviceService deviceService;
    private final ClientIpResolver clientIpResolver;

    public DeviceController(DeviceService deviceService, ClientIpResolver clientIpResolver) {
        this.deviceService = deviceService;
        this.clientIpResolver = clientIpResolver;
    }

    @PostMapping("/register")
    public Device register(
            @AuthenticationPrincipal TrackerPrincipal principal,
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest
    ) {
        return deviceService.register(
                principal.userId(),
                request,
                clientIpResolver.resolve(httpRequest)
        );
    }

    @GetMapping
    public List<Device> list(@AuthenticationPrincipal TrackerPrincipal principal) {
        return deviceService.list(principal.userId());
    }

    @PostMapping("/{deviceId}/heartbeat")
    public Device heartbeat(
            @AuthenticationPrincipal TrackerPrincipal principal,
            @PathVariable Long deviceId,
            @RequestBody(required = false) HeartbeatRequest request,
            HttpServletRequest httpRequest
    ) {
        return deviceService.heartbeat(
                principal.userId(),
                deviceId,
                request,
                clientIpResolver.resolve(httpRequest)
        );
    }

    @PostMapping("/{deviceId}/shutdown")
    public Device shutdown(
            @AuthenticationPrincipal TrackerPrincipal principal,
            @PathVariable Long deviceId,
            @RequestBody(required = false) ShutdownRequest request
    ) {
        return deviceService.shutdown(principal.userId(), deviceId, request);
    }

    @PostMapping("/{deviceId}/uninstall")
    public Device uninstall(
            @AuthenticationPrincipal TrackerPrincipal principal,
            @PathVariable Long deviceId,
            @RequestBody(required = false) UninstallRequest request
    ) {
        return deviceService.uninstall(principal.userId(), deviceId, request);
    }
}
