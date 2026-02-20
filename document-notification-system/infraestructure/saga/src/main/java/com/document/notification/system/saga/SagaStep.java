package com.document.notification.system.saga;

public interface SagaStep<T> {
    void execute(T data) throws Exception;

    void compensate(T data) throws Exception;
}
