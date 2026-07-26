package com.tracker.server.service;

import com.tracker.server.dto.AdminRecordFilter;
import com.tracker.server.entity.ActiveWindowActivity;
import com.tracker.server.entity.Device;
import com.tracker.server.entity.DeviceSession;
import com.tracker.server.entity.IdleActivity;
import com.tracker.server.entity.ProcessActivity;
import com.tracker.server.repository.ActiveWindowActivityRepository;
import com.tracker.server.repository.DeviceRepository;
import com.tracker.server.repository.DeviceSessionRepository;
import com.tracker.server.repository.IdleActivityRepository;
import com.tracker.server.repository.ProcessActivityRepository;
import com.tracker.server.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.ToLongFunction;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
public class AdminService {
    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final ProcessActivityRepository processRepository;
    private final ActiveWindowActivityRepository windowRepository;
    private final IdleActivityRepository idleRepository;
    private final DeviceSessionRepository sessionRepository;

    public AdminService(
            UserRepository userRepository,
            DeviceRepository deviceRepository,
            ProcessActivityRepository processRepository,
            ActiveWindowActivityRepository windowRepository,
            IdleActivityRepository idleRepository,
            DeviceSessionRepository sessionRepository
    ) {
        this.userRepository = userRepository;
        this.deviceRepository = deviceRepository;
        this.processRepository = processRepository;
        this.windowRepository = windowRepository;
        this.idleRepository = idleRepository;
        this.sessionRepository = sessionRepository;
    }

    public Map<String, Long> summary() {
        Map<String, Long> result = new LinkedHashMap<>();
        result.put("totalUsers", userRepository.countByRole("USER"));
        result.put("totalDevices", deviceRepository.count());
        result.put("onlineDevices", deviceRepository.countByStatus("ONLINE"));
        result.put("offlineDevices", deviceRepository.countByStatus("OFFLINE"));
        result.put("shutdownDevices", deviceRepository.countByStatus("SHUTDOWN"));
        return result;
    }

    public List<?> users() {
        return userRepository.findAllByOrderByUsernameAsc()
                .stream()
                .filter(user -> "USER".equals(user.getRole()))
                .toList();
    }

    public List<Device> devices(Long userId, String status) {
        Stream<Device> stream = (userId == null
                ? deviceRepository.findAllByOrderByIdDesc()
                : deviceRepository.findByUser_IdOrderByIdDesc(userId)).stream();
        if (hasText(status)) {
            stream = stream.filter(device -> status.equalsIgnoreCase(device.getStatus()));
        }
        return stream.toList();
    }

    public Map<String, Object> today(Long deviceId) {
        requireDevice(deviceId);
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("runningProcesses", processRepository.findByDevice_IdAndStatus(deviceId, "RUNNING"));
        result.put("activeWindows", windowRepository.findByDevice_IdOrderByStartTimeDesc(deviceId)
                .stream()
                .filter(item -> inRange(item.getStartTime(), start, end))
                .toList());
        result.put("idle", idleRepository.findByDevice_IdOrderByStartTimeDesc(deviceId)
                .stream()
                .filter(item -> inRange(item.getStartTime(), start, end))
                .toList());
        result.put("lastSession", sessionRepository.findByDevice_IdOrderByStartupTimeDesc(deviceId)
                .stream()
                .findFirst()
                .orElse(null));
        return result;
    }

    public RecordResponse records(Long deviceId, String type, AdminRecordFilter filter) {
        requireDevice(deviceId);
        DateRange dateRange = dateRange(filter);
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "processes" -> processRecords(deviceId, filter, dateRange);
            case "windows" -> windowRecords(deviceId, filter, dateRange);
            case "idle" -> idleRecords(deviceId, filter, dateRange);
            case "sessions" -> sessionRecords(deviceId, filter, dateRange);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown record type");
        };
    }

    private RecordResponse processRecords(Long deviceId, AdminRecordFilter filter, DateRange range) {
        Stream<ProcessActivity> stream = processRepository.findByDevice_IdOrderByStartTimeDesc(deviceId)
                .stream()
                .filter(item -> inRange(item.getStartTime(), range.start(), range.end()))
                .filter(item -> !hasText(filter.getProcessName())
                        || contains(item.getProcessName(), filter.getProcessName()))
                .filter(item -> !hasText(filter.getWindowName())
                        || contains(item.getWindowName(), filter.getWindowName()))
                .filter(item -> filter.getPid() == null || item.getPid() == filter.getPid())
                .filter(item -> commonFilter(
                        item.getStartTime(),
                        item.getEndTime(),
                        item.getDurationSeconds(),
                        item.getDisplayStatus(),
                        filter
                ));
        List<ProcessActivity> items = stream.toList();
        return response(items, filter, ProcessActivity::getDurationSeconds);
    }

    private RecordResponse windowRecords(Long deviceId, AdminRecordFilter filter, DateRange range) {
        Stream<ActiveWindowActivity> stream = windowRepository.findByDevice_IdOrderByStartTimeDesc(deviceId)
                .stream()
                .filter(item -> inRange(item.getStartTime(), range.start(), range.end()))
                .filter(item -> !hasText(filter.getWindowName())
                        || contains(item.getWindowTitle(), filter.getWindowName()))
                .filter(item -> commonFilter(
                        item.getStartTime(),
                        item.getEndTime(),
                        item.getDurationSeconds(),
                        item.getDisplayStatus(),
                        filter
                ));
        List<ActiveWindowActivity> items = stream.toList();
        return response(items, filter, ActiveWindowActivity::getDurationSeconds);
    }

    private RecordResponse idleRecords(Long deviceId, AdminRecordFilter filter, DateRange range) {
        Stream<IdleActivity> stream = idleRepository.findByDevice_IdOrderByStartTimeDesc(deviceId)
                .stream()
                .filter(item -> inRange(item.getStartTime(), range.start(), range.end()))
                .filter(item -> commonFilter(
                        item.getStartTime(),
                        item.getEndTime(),
                        item.getDurationSeconds(),
                        item.getDisplayStatus(),
                        filter
                ));
        List<IdleActivity> items = stream.toList();
        return response(items, filter, IdleActivity::getDurationSeconds);
    }

    private RecordResponse sessionRecords(Long deviceId, AdminRecordFilter filter, DateRange range) {
        Stream<DeviceSession> stream = sessionRepository.findByDevice_IdOrderByStartupTimeDesc(deviceId)
                .stream()
                .filter(item -> inRange(item.getStartupTime(), range.start(), range.end()))
                .filter(item -> commonFilter(
                        item.getStartupTime(),
                        item.getShutdownTime(),
                        item.getDurationSeconds(),
                        item.getDisplayStatus(),
                        filter
                ));
        List<DeviceSession> items = stream.toList();
        return response(items, filter, DeviceSession::getDurationSeconds);
    }

    private boolean commonFilter(
            LocalDateTime start,
            LocalDateTime end,
            long duration,
            String status,
            AdminRecordFilter filter
    ) {
        return (filter.getStartTime() == null || !start.isBefore(filter.getStartTime()))
                && (filter.getEndTime() == null || end != null && !end.isAfter(filter.getEndTime()))
                && (filter.getMinDuration() == null || duration >= filter.getMinDuration())
                && (filter.getMaxDuration() == null || duration <= filter.getMaxDuration())
                && (!hasText(filter.getStatus()) || filter.getStatus().equalsIgnoreCase(status));
    }

    private <T> RecordResponse response(
            List<T> items,
            AdminRecordFilter filter,
            ToLongFunction<T> duration
    ) {
        Long total = filter.hasDataFilter()
                ? items.stream().mapToLong(duration).sum()
                : null;
        return new RecordResponse(items, filter.hasDataFilter(), total);
    }

    private DateRange dateRange(AdminRecordFilter filter) {
        String value = filter.getRange() == null ? "today" : filter.getRange().toLowerCase(Locale.ROOT);
        LocalDate today = LocalDate.now();
        return switch (value) {
            case "all" -> new DateRange(null, null);
            case "yesterday" -> day(today.minusDays(1));
            case "before-yesterday" -> day(today.minusDays(2));
            case "custom" -> {
                LocalDate from = filter.getFromDate() == null ? today : filter.getFromDate();
                LocalDate to = filter.getToDate() == null ? from : filter.getToDate();
                if (to.isBefore(from)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid custom date range");
                }
                yield new DateRange(from.atStartOfDay(), to.plusDays(1).atStartOfDay());
            }
            default -> day(today);
        };
    }

    private DateRange day(LocalDate date) {
        return new DateRange(date.atStartOfDay(), date.plusDays(1).atStartOfDay());
    }

    private boolean inRange(LocalDateTime value, LocalDateTime start, LocalDateTime end) {
        return (start == null || !value.isBefore(start)) && (end == null || value.isBefore(end));
    }

    private boolean contains(String source, String query) {
        return source != null && source.toLowerCase(Locale.ROOT)
                .contains(query.trim().toLowerCase(Locale.ROOT));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Device requireDevice(Long deviceId) {
        return deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));
    }

    public record RecordResponse(List<?> items, boolean filtered, Long totalDurationSeconds) {
    }

    private record DateRange(LocalDateTime start, LocalDateTime end) {
    }
}
