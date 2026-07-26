package com.tracker.server.controller;

import com.tracker.server.dto.AdminRecordFilter;
import com.tracker.server.entity.Device;
import com.tracker.server.service.AdminService;
import com.tracker.server.service.AdminService.RecordResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/summary")
    public Map<String, Long> summary() {
        return adminService.summary();
    }

    @GetMapping("/users")
    public List<?> users() {
        return adminService.users();
    }

    @GetMapping("/devices")
    public List<Device> devices(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String status
    ) {
        return adminService.devices(userId, status);
    }

    @GetMapping("/devices/{deviceId}/today")
    public Map<String, Object> today(@PathVariable Long deviceId) {
        return adminService.today(deviceId);
    }

    @GetMapping("/devices/{deviceId}/records/{type}")
    public RecordResponse records(
            @PathVariable Long deviceId,
            @PathVariable String type,
            @ModelAttribute AdminRecordFilter filter
    ) {
        return adminService.records(deviceId, type, filter);
    }
}
