package com.tracker.server.util;

import java.time.LocalDateTime;
import java.time.ZoneId;

public class DateTimeUtil {

    private static final ZoneId INDIA =
            ZoneId.of("Asia/Kolkata");

    private DateTimeUtil() {
    }

    public static LocalDateTime now() {

        return LocalDateTime.now(INDIA);
    }
}