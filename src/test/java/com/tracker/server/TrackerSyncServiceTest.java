package com.tracker.server;

import com.tracker.server.dto.SyncDtos.BatchRequest;
import com.tracker.server.dto.SyncDtos.IdleItem;
import com.tracker.server.dto.SyncDtos.ProcessItem;
import com.tracker.server.dto.SyncDtos.SessionItem;
import com.tracker.server.dto.SyncDtos.WindowItem;
import com.tracker.server.entity.Device;
import com.tracker.server.entity.User;
import com.tracker.server.repository.ActiveWindowActivityRepository;
import com.tracker.server.repository.DeviceRepository;
import com.tracker.server.repository.DeviceSessionRepository;
import com.tracker.server.repository.IdleActivityRepository;
import com.tracker.server.repository.ProcessActivityRepository;
import com.tracker.server.repository.UserRepository;
import com.tracker.server.scheduler.DeviceStatusScheduler;
import com.tracker.server.service.TrackerSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TrackerSyncServiceTest {
    @Autowired
    private TrackerSyncService syncService;

    @Autowired
    private DeviceStatusScheduler statusScheduler;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private ProcessActivityRepository processRepository;

    @Autowired
    private ActiveWindowActivityRepository windowRepository;

    @Autowired
    private IdleActivityRepository idleRepository;

    @Autowired
    private DeviceSessionRepository sessionRepository;

    private User user;
    private Device device;

    @BeforeEach
    void setUp() {
        processRepository.deleteAll();
        windowRepository.deleteAll();
        idleRepository.deleteAll();
        sessionRepository.deleteAll();
        deviceRepository.deleteAll();
        userRepository.deleteAll();

        user = new User();
        user.setUsername("agent-user");
        user.setRole("USER");
        user = userRepository.save(user);

        device = new Device();
        device.setInstallationId("install-1");
        device.setMachineName("workstation");
        device.setStatus("ONLINE");
        device.setOnline(true);
        device.setLastSeen(LocalDateTime.now());
        device.setUser(user);
        device = deviceRepository.save(device);
    }

    @Test
    void retryAndCloseUpdateTheSameProcessRow() {
        LocalDateTime start = micros(LocalDateTime.now().minusMinutes(5));
        BatchRequest running = new BatchRequest();
        running.getProcesses().add(process("local-process", start, null));

        syncService.sync(user.getId(), device.getId(), running);
        syncService.sync(user.getId(), device.getId(), running);

        assertThat(processRepository.count()).isEqualTo(1);
        Long serverId = processRepository.findAll().getFirst().getId();

        BatchRequest closed = new BatchRequest();
        closed.getProcesses().add(process("local-process", start, start.plusMinutes(3)));
        syncService.sync(user.getId(), device.getId(), closed);

        assertThat(processRepository.count()).isEqualTo(1);
        assertThat(processRepository.findById(serverId).orElseThrow())
                .extracting("status", "endTime")
                .containsExactly("CLOSED", start.plusMinutes(3));
    }

    @Test
    void onlyOneWindowIdleAndSessionRemainRunning() {
        LocalDateTime start = micros(LocalDateTime.now().minusMinutes(10));
        BatchRequest first = new BatchRequest();
        first.getWindows().add(window("window-1", "Editor", start));
        first.getIdle().add(idle("idle-1", start));
        first.getSessions().add(session("session-1", start));
        syncService.sync(user.getId(), device.getId(), first);

        BatchRequest second = new BatchRequest();
        second.getWindows().add(window("window-2", "Browser", start.plusMinutes(2)));
        second.getIdle().add(idle("idle-2", start.plusMinutes(2)));
        second.getSessions().add(session("session-2", start.plusMinutes(2)));
        syncService.sync(user.getId(), device.getId(), second);

        assertThat(windowRepository.findByDevice_IdAndStatus(device.getId(), "RUNNING")).hasSize(1);
        assertThat(windowRepository.findAll()).hasSize(2);
        assertThat(idleRepository.findByDevice_IdAndStatus(device.getId(), "RUNNING")).hasSize(1);
        assertThat(idleRepository.findAll()).hasSize(1);
        assertThat(sessionRepository.findByDevice_IdAndStatus(device.getId(), "RUNNING")).hasSize(1);
        assertThat(sessionRepository.findAll()).hasSize(1);
    }

    @Test
    void heartbeatTimeoutOnlyMarksDeviceOffline() {
        LocalDateTime start = micros(LocalDateTime.now().minusMinutes(2));
        BatchRequest request = new BatchRequest();
        request.getProcesses().add(process("running", start, null));
        syncService.sync(user.getId(), device.getId(), request);

        Device saved = deviceRepository.findById(device.getId()).orElseThrow();
        saved.setLastSeen(LocalDateTime.now().minusMinutes(2));
        saved.setOnline(true);
        saved.setStatus("ONLINE");
        deviceRepository.saveAndFlush(saved);

        statusScheduler.markMissingHeartbeatsOffline();

        Device offline = deviceRepository.findById(device.getId()).orElseThrow();
        var activity = processRepository.findAll().getFirst();
        assertThat(offline.isOnline()).isFalse();
        assertThat(offline.getStatus()).isEqualTo("OFFLINE");
        assertThat(activity.getStatus()).isEqualTo("RUNNING");
        assertThat(activity.getEndTime()).isNull();
    }

    private ProcessItem process(String localId, LocalDateTime start, LocalDateTime end) {
        ProcessItem item = new ProcessItem();
        item.setLocalId(localId);
        item.setPid(4242);
        item.setProcessName("editor.exe");
        item.setWindowName("Project");
        item.setStartTime(start);
        item.setEndTime(end);
        item.setDurationSeconds(end == null ? 120 : 180);
        item.setStatus(end == null ? "RUNNING" : "CLOSED");
        return item;
    }

    private WindowItem window(String localId, String title, LocalDateTime start) {
        WindowItem item = new WindowItem();
        item.setLocalId(localId);
        item.setWindowTitle(title);
        item.setStartTime(start);
        item.setDurationSeconds(30);
        item.setStatus("RUNNING");
        return item;
    }

    private IdleItem idle(String localId, LocalDateTime start) {
        IdleItem item = new IdleItem();
        item.setLocalId(localId);
        item.setStartTime(start);
        item.setDurationSeconds(30);
        item.setStatus("RUNNING");
        return item;
    }

    private SessionItem session(String localId, LocalDateTime start) {
        SessionItem item = new SessionItem();
        item.setLocalId(localId);
        item.setStartupTime(start);
        item.setDurationSeconds(30);
        item.setStatus("RUNNING");
        return item;
    }

    private LocalDateTime micros(LocalDateTime value) {
        return value.withNano(value.getNano() / 1_000 * 1_000);
    }
}
