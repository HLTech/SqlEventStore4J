package com.hltech.store.versioning;

import com.hltech.store.EventStoreException;

public class EventTypeMappingException extends EventStoreException {

    EventTypeMappingException(String message) {
        super(message);
    }

}
