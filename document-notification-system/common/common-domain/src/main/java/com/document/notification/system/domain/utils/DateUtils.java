package com.document.notification.system.domain.utils;

import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 9/11/2025
 */
public class DateUtils {
    public static final String ZONE_ID_UTC = "UTC";

    public static ZonedDateTime getZoneDateTimeByZoneId(String zoneId) {
        return ZonedDateTime.now(ZoneId.of(ZONE_ID_UTC));
    }
}
