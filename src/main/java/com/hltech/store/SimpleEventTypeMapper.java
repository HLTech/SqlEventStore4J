package com.hltech.store;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SimpleEventTypeMapper implements EventTypeMapper {

    private final Map<NameAndVersion, Class<? extends Event>> eventNameAndVersionToTypeMap = new HashMap<>();
    private final Map<Class<? extends Event>, NameAndVersion> eventTypeToNameAndVersionMap = new HashMap<>();

    public SimpleEventTypeMapper() {
    }

    public SimpleEventTypeMapper(Collection<TypeNameAndVersion> mappings) {
        registerMappings(mappings);
    }

    @Override
    public <T extends Event> String toName(Class<T> eventType) {
        NameAndVersion nameAndVersion = eventTypeToNameAndVersionMap.get(eventType);
        if (nameAndVersion == null) {
            throw new EventTypeMappingException("Mapping to event name not found for event type: " + eventType);
        }
        return nameAndVersion.getName();
    }

    @Override
    public <T extends Event> short toVersion(Class<T> eventType) {
        NameAndVersion nameAndVersion = eventTypeToNameAndVersionMap.get(eventType);
        if (nameAndVersion == null) {
            throw new EventTypeMappingException("Mapping to event version not found for event type: " + eventType);
        }
        return nameAndVersion.getVersion();
    }

    @Override
    public  Class<? extends Event> toType(String eventName, short eventVersion) {
        Class<? extends Event> eventType = eventNameAndVersionToTypeMap.get(new NameAndVersion(eventName, eventVersion));
        if (eventType == null) {
            throw new EventTypeMappingException("Mapping to event type not found for event name: " + eventName + " and event version: " + eventVersion);
        }
        return eventType;
    }

    @Override
    public <T extends Event> void registerMapping(TypeNameAndVersion mapping) {
        eventNameAndVersionToTypeMap.put(mapping.getNameAndVersion(), mapping.getType());
        eventTypeToNameAndVersionMap.put(mapping.getType(), mapping.getNameAndVersion());
    }

}
