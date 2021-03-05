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
        return eventTypeToNameAndVersionMap.get(eventType).getName();
    }

    @Override
    public <T extends Event> short toVersion(Class<T> eventType) {
        return eventTypeToNameAndVersionMap.get(eventType).getVersion();
    }

    @Override
    public  Class<? extends Event> toType(String eventName, short eventVersion) {
        return eventNameAndVersionToTypeMap.get(new NameAndVersion(eventName, eventVersion));
    }

    public <T extends Event> void registerMappings(Collection<TypeNameAndVersion> mappings) {
        mappings.forEach(typeNameAndVersion -> {
            eventNameAndVersionToTypeMap.put(typeNameAndVersion.getNameAndVersion(), typeNameAndVersion.getType());
            eventTypeToNameAndVersionMap.put(typeNameAndVersion.getType(), typeNameAndVersion.getNameAndVersion());
        });
    }

    public <T extends Event> void registerMapping(TypeNameAndVersion mapping) {
        eventNameAndVersionToTypeMap.put(mapping.getNameAndVersion(), mapping.getType());
        eventTypeToNameAndVersionMap.put(mapping.getType(), mapping.getNameAndVersion());
    }

    public <T extends Event> void registerMapping(
            Class<T> eventType,
            String eventName,
            short eventVersion
    ) {
        registerMapping(new TypeNameAndVersion(eventType, new NameAndVersion(eventName, eventVersion)));
    }

}
