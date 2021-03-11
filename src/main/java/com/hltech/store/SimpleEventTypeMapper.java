package com.hltech.store;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SimpleEventTypeMapper<E> implements EventTypeMapper<E> {

    private final Map<NameAndVersion, Class<? extends E>> eventNameAndVersionToTypeMap = new HashMap<>();
    private final Map<Class<? extends E>, NameAndVersion> eventTypeToNameAndVersionMap = new HashMap<>();

    public SimpleEventTypeMapper() {
    }

    public SimpleEventTypeMapper(Collection<TypeNameAndVersion<E>> mappings) {
        registerMappings(mappings);
    }

    @Override
    public String toName(Class<? extends E> eventType) {
        NameAndVersion nameAndVersion = eventTypeToNameAndVersionMap.get(eventType);
        if (nameAndVersion == null) {
            throw new EventTypeMappingException("Mapping to event name not found for event type: " + eventType);
        }
        return nameAndVersion.getName();
    }

    @Override
    public short toVersion(Class<? extends E> eventType) {
        NameAndVersion nameAndVersion = eventTypeToNameAndVersionMap.get(eventType);
        if (nameAndVersion == null) {
            throw new EventTypeMappingException("Mapping to event version not found for event type: " + eventType);
        }
        return nameAndVersion.getVersion();
    }

    @Override
    public Class<? extends E> toType(String eventName, short eventVersion) {
        Class<? extends E> eventType = eventNameAndVersionToTypeMap.get(new NameAndVersion(eventName, eventVersion));
        if (eventType == null) {
            throw new EventTypeMappingException("Mapping to event type not found for event name: " + eventName + " and event version: " + eventVersion);
        }
        return eventType;
    }

    @Override
    public void registerMapping(TypeNameAndVersion<? extends E> mapping) {
        eventNameAndVersionToTypeMap.put(mapping.getNameAndVersion(), mapping.getType());
        eventTypeToNameAndVersionMap.put(mapping.getType(), mapping.getNameAndVersion());
    }

}
