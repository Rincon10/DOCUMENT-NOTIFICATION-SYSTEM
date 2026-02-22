package com.document.notification.system.saga;

public interface SagaStep<T> {
    void execute(T data);

    void compensate(T data);
}
