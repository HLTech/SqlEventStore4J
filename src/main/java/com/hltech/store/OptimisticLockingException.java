package com.hltech.store;

public class OptimisticLockingException extends EventStoreException {

    OptimisticLockingException(String message) {
        super(message);
    }

    OptimisticLockingException(String message, Throwable cause) {
        super(message, cause);
    }

}
