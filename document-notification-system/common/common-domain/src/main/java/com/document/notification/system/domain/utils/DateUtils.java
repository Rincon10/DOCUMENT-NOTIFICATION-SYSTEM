package com.document.notification.system.domain.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 9/11/2025
 */
public class  DateUtils {
    public static final String ZONE_ID_UTC = "UTC";

    private DateUtils() {
    }

    public static ZonedDateTime getZoneDateTimeByZoneId(String zoneId) {
        return ZonedDateTime.now(ZoneId.of(zoneId));
    }

    public static ZonedDateTime getZoneDateTimeByUTCZoneId() {
        return getZoneDateTimeByZoneId(ZONE_ID_UTC);
    }

    public static String formatDate(LocalDate date) {
        if (Objects.isNull(date)) {
            return null;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return date.format(formatter);

    }

    public static LocalDate parseStringDatetoLocalDate(String date) {
        if (Objects.isNull(date) || date.isBlank()) {
            return null;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return LocalDate.parse(date, formatter);
    }

    /**
     * Converts timestamp in milliseconds to Instant
     */
    public static LocalDate convertTimestampToLocalDate(Long timestamp) {
        if (timestamp == null) {
            return null;
        }
        return convertInstantToLocalDate(Instant.ofEpochMilli(timestamp));
    }

    /**
     * Converts days since epoch to LocalDate
     */
    public static LocalDate convertInstantToLocalDate(Instant instant) {
        if (instant == null) {
            return null;
        }
        return LocalDate.ofInstant(instant,getZoneDateTimeByUTCZoneId().getZone());
    }
}
