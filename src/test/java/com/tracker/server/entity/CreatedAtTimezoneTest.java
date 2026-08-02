package com.tracker.server.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

class CreatedAtTimezoneTest {

    @Test
    void createdAtUsesIndiaTimeWhenServerRunsInUtc() {
        TimeZone originalTimeZone = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            ZoneId india = ZoneId.of("Asia/Kolkata");
            LocalDateTime before = LocalDateTime.now(india).minusSeconds(1);

            User user = new User();
            user.onCreate();
            Device device = new Device();
            device.onCreate();

            LocalDateTime after = LocalDateTime.now(india).plusSeconds(1);
            assertThat(user.getCreatedAt()).isBetween(before, after);
            assertThat(device.getCreatedAt()).isBetween(before, after);
        } finally {
            TimeZone.setDefault(originalTimeZone);
        }
    }
}
