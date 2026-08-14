package com.tracker.server.service;

import com.tracker.server.dto.SyncDtos.BatchRequest;
import com.tracker.server.dto.SyncDtos.BatchResponse;
import com.tracker.server.dto.SyncDtos.IdleItem;
import com.tracker.server.dto.SyncDtos.ProcessItem;
import com.tracker.server.dto.SyncDtos.SessionItem;
import com.tracker.server.dto.SyncDtos.WindowItem;
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
import com.tracker.server.util.DateTimeUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class TrackerSyncService {
    private final DeviceRepository deviceRepository;
    private final ProcessActivityRepository processRepository;
    private final ActiveWindowActivityRepository windowRepository;
    private final IdleActivityRepository idleRepository;
    private final DeviceSessionRepository sessionRepository;

    public TrackerSyncService(
            DeviceRepository deviceRepository,
            ProcessActivityRepository processRepository,
            ActiveWindowActivityRepository windowRepository,
            IdleActivityRepository idleRepository,
            DeviceSessionRepository sessionRepository
    ) {
        this.deviceRepository = deviceRepository;
        this.processRepository = processRepository;
        this.windowRepository = windowRepository;
        this.idleRepository = idleRepository;
        this.sessionRepository = sessionRepository;
    }

    @Transactional
    public BatchResponse sync(Long userId, Long deviceId, BatchRequest request) {
        Device device = deviceRepository.findByIdForUpdate(deviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));
        if (!device.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Device does not belong to user");
        }
        if (device.isUninstalled()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Device is uninstalled");
        }

        BatchResponse response = new BatchResponse();
        sortedProcesses(request.getProcesses()).forEach(item ->
                response.getProcesses().put(item.getLocalId(), upsertProcess(device, item).getId()));
        sortedWindows(request.getWindows()).forEach(item ->
                response.getWindows().put(item.getLocalId(), upsertWindow(device, item).getId()));
        sortedIdle(request.getIdle()).forEach(item ->
                response.getIdle().put(item.getLocalId(), upsertIdle(device, item).getId()));
        sortedSessions(request.getSessions()).forEach(item ->
                response.getSessions().put(item.getLocalId(), upsertSession(device, item).getId()));

        device.setOnline(true);
        device.setStatus("ONLINE");
        device.setShutdownAt(null);
        device.setLastSeen(DateTimeUtil.now());
        return response;
    }

    private ProcessActivity upsertProcess(Device device, ProcessItem item) {
        ProcessActivity activity = processRepository.findByLocalId(item.getLocalId())
                .or(() -> processRepository
                        .findFirstByDevice_IdAndPidAndProcessNameAndStartTimeAndEndTimeIsNull(
                                device.getId(), item.getPid(), item.getProcessName(), item.getStartTime()))
                .orElseGet(ProcessActivity::new);
        verifyDevice(activity.getDevice(), device);
        activity.setLocalId(item.getLocalId());
        activity.setDevice(device);
        activity.setUser(device.getUser());
        activity.setPid(item.getPid());
        activity.setProcessName(item.getProcessName());
        activity.setWindowName(item.getWindowName());
        activity.setStartTime(item.getStartTime());
        applyProcessState(activity, item);
        return processRepository.save(activity);
    }

    private void applyProcessState(ProcessActivity activity, ProcessItem item) {
        if (item.getEndTime() == null) {
            activity.setEndTime(null);
            activity.setStatus("RUNNING");
            activity.setDurationSeconds(Math.max(activity.getDurationSeconds(), nonNegative(item.getDurationSeconds())));
            return;
        }
        activity.setEndTime(item.getEndTime());
        activity.setStatus("CLOSED");
        activity.setDurationSeconds(duration(item.getStartTime(), item.getEndTime(), item.getDurationSeconds()));
    }

    private ActiveWindowActivity upsertWindow(Device device, WindowItem item) {
        ActiveWindowActivity activity = windowRepository.findByLocalId(item.getLocalId())
                .or(() -> windowRepository
                        .findFirstByDevice_IdAndWindowTitleAndStartTimeAndEndTimeIsNull(
                                device.getId(), item.getWindowTitle(), item.getStartTime()))
                .orElseGet(ActiveWindowActivity::new);
        verifyDevice(activity.getDevice(), device);

        if (item.getEndTime() == null) {
            closeOtherWindows(device, activity.getId(), item.getStartTime());
        }

        activity.setLocalId(item.getLocalId());
        activity.setDevice(device);
        activity.setUser(device.getUser());
        activity.setWindowTitle(item.getWindowTitle());
        activity.setStartTime(item.getStartTime());
        if (item.getEndTime() == null) {
            activity.setEndTime(null);
            activity.setStatus("RUNNING");
            activity.setDurationSeconds(Math.max(activity.getDurationSeconds(), nonNegative(item.getDurationSeconds())));
        } else {
            activity.setEndTime(item.getEndTime());
            activity.setStatus("CLOSED");
            activity.setDurationSeconds(duration(item.getStartTime(), item.getEndTime(), item.getDurationSeconds()));
        }
        return windowRepository.save(activity);
    }

    private void closeOtherWindows(Device device, Long currentId, LocalDateTime nextStart) {
        for (ActiveWindowActivity running : windowRepository.findByDevice_IdAndStatus(device.getId(), "RUNNING")) {
            if (currentId != null && currentId.equals(running.getId())) {
                continue;
            }
            LocalDateTime end = nextStart.isBefore(running.getStartTime()) ? running.getStartTime() : nextStart;
            running.setEndTime(end);
            running.setDurationSeconds(secondsBetween(running.getStartTime(), end));
            running.setStatus("CLOSED");
            windowRepository.save(running);
        }
    }

    private IdleActivity upsertIdle(Device device, IdleItem item) {
        IdleActivity activity = idleRepository.findByLocalId(item.getLocalId())
                .or(() -> idleRepository
                        .findFirstByDevice_IdAndStartTimeAndEndTimeIsNull(device.getId(), item.getStartTime()))
                .orElse(null);

        if (activity == null && item.getEndTime() == null) {
            activity = idleRepository.findByDevice_IdAndStatus(device.getId(), "RUNNING")
                    .stream()
                    .findFirst()
                    .orElse(null);
        }
        if (activity == null) {
            activity = new IdleActivity();
        }
        verifyDevice(activity.getDevice(), device);
        if (activity.getLocalId() == null) {
            activity.setLocalId(item.getLocalId());
        }
        activity.setDevice(device);
        activity.setUser(device.getUser());
        if (activity.getStartTime() == null) {
            activity.setStartTime(item.getStartTime());
        }
        if (item.getEndTime() == null) {
            activity.setEndTime(null);
            activity.setStatus("RUNNING");
            activity.setDurationSeconds(Math.max(activity.getDurationSeconds(), nonNegative(item.getDurationSeconds())));
        } else {
            activity.setEndTime(item.getEndTime());
            activity.setStatus("CLOSED");
            activity.setDurationSeconds(duration(activity.getStartTime(), item.getEndTime(), item.getDurationSeconds()));
        }
        return idleRepository.save(activity);
    }

    private DeviceSession upsertSession(Device device, SessionItem item) {
        DeviceSession session = sessionRepository.findByLocalId(item.getLocalId())
                .or(() -> sessionRepository
                        .findFirstByDevice_IdAndStartupTimeAndShutdownTimeIsNull(device.getId(), item.getStartupTime()))
                .orElse(null);

        if (session == null) {
            session = new DeviceSession();
        }
        verifyDevice(session.getDevice(), device);
        if (item.getShutdownTime() == null) {
            closeOtherSessions(device, session.getId(), item.getStartupTime());
        }
        if (session.getLocalId() == null) {
            session.setLocalId(item.getLocalId());
        }
        session.setDevice(device);
        session.setUser(device.getUser());
        if (session.getStartupTime() == null) {
            session.setStartupTime(item.getStartupTime());
        }
        if (item.getShutdownTime() == null) {
            session.setShutdownTime(null);
            session.setStatus("RUNNING");
            session.setDurationSeconds(Math.max(session.getDurationSeconds(), nonNegative(item.getDurationSeconds())));
        } else {
            session.setShutdownTime(item.getShutdownTime());
            session.setStatus("SHUTDOWN");
            session.setDurationSeconds(duration(
                    session.getStartupTime(), item.getShutdownTime(), item.getDurationSeconds()));
        }
        return sessionRepository.save(session);
    }

    private void closeOtherSessions(Device device, Long currentId, LocalDateTime nextStart) {
        for (DeviceSession running : sessionRepository.findByDevice_IdAndStatus(device.getId(), "RUNNING")) {
            if (currentId != null && currentId.equals(running.getId())) {
                continue;
            }
            LocalDateTime end = nextStart.isBefore(running.getStartupTime()) ? running.getStartupTime() : nextStart;
            running.setShutdownTime(end);
            running.setDurationSeconds(secondsBetween(running.getStartupTime(), end));
            running.setStatus("SHUTDOWN");
            sessionRepository.save(running);
        }
    }

    private void verifyDevice(Device existing, Device requested) {
        if (existing != null && !existing.getId().equals(requested.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Local ID belongs to another device");
        }
    }

    private long duration(LocalDateTime start, LocalDateTime end, long supplied) {
        return Math.max(nonNegative(supplied), secondsBetween(start, end));
    }

    private long secondsBetween(LocalDateTime start, LocalDateTime end) {
        return Math.max(0, Duration.between(start, end).toSeconds());
    }

    private long nonNegative(long value) {
        return Math.max(0, value);
    }

    private List<ProcessItem> sortedProcesses(List<ProcessItem> source) {
        List<ProcessItem> result = new ArrayList<>(source == null ? List.of() : source);
        result.removeIf(item -> item.getWindowName() == null || item.getWindowName().isBlank());
        result.sort(Comparator.comparing(ProcessItem::getStartTime)
                .thenComparing(item -> item.getEndTime() == null));
        return result;
    }

    private List<WindowItem> sortedWindows(List<WindowItem> source) {
        List<WindowItem> result = new ArrayList<>(source == null ? List.of() : source);
        result.sort(Comparator.comparing(WindowItem::getStartTime)
                .thenComparing(item -> item.getEndTime() == null));
        return result;
    }

    private List<IdleItem> sortedIdle(List<IdleItem> source) {
        List<IdleItem> result = new ArrayList<>(source == null ? List.of() : source);
        result.sort(Comparator.comparing(IdleItem::getStartTime)
                .thenComparing(item -> item.getEndTime() == null));
        return result;
    }

    private List<SessionItem> sortedSessions(List<SessionItem> source) {
        List<SessionItem> result = new ArrayList<>(source == null ? List.of() : source);
        result.sort(Comparator.comparing(SessionItem::getStartupTime)
                .thenComparing(item -> item.getShutdownTime() == null));
        return result;
    }
}
