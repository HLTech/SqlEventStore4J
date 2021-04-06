package com.hltech.store;

public class EventStoreException extends RuntimeException {

    EventStoreException(String message) {
        super(message);
    }

    EventStoreException(String message, Throwable cause) {
        super(message, cause);
    }

}
