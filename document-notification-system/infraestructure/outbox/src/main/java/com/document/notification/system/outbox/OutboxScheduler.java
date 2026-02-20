package com.document.notification.system.outbox;

public interface OutboxScheduler {
    void processOutboxMessage();
}
