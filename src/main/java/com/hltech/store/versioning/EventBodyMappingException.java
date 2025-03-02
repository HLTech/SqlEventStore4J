package com.hltech.store.versioning;

import com.hltech.store.EventStoreException;

public class EventBodyMappingException extends EventStoreException {

    EventBodyMappingException(String eventJson, Class eventType, Throwable cause) {
        super(String.format("Could not create event of type %s from json %s", eventType.getTypeName(), eventJson), cause);
    }

    EventBodyMappingException(String eventJson, String eventName, Throwable cause) {
        super(String.format("Could not create event of name %s from json %s", eventName, eventJson), cause);
    }

    EventBodyMappingException(Object event, Throwable cause) {
        super(String.format("Could not create json from event %s", event), cause);
    }

}
