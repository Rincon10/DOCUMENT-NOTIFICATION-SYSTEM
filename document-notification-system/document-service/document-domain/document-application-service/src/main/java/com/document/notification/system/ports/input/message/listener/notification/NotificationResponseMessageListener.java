package com.document.notification.system.ports.input.message.listener.notification;

import com.document.notification.system.dto.message.NotificationResponse;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 14/03/2026
 */
public interface NotificationResponseMessageListener {

    void notificationCompleted(NotificationResponse notificationResponse);

    void notificationFailed(NotificationResponse notificationResponse);

}
