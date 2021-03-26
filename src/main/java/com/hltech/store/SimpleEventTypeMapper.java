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
    public int toVersion(Class<? extends E> eventType) {
        NameAndVersion nameAndVersion = eventTypeToNameAndVersionMap.get(eventType);
        if (nameAndVersion == null) {
            throw new EventTypeMappingException("Mapping to event version not found for event type: " + eventType);
        }
        return nameAndVersion.getVersion();
    }

    @Override
    public Class<? extends E> toType(String eventName, int eventVersion) {
        Class<? extends E> eventType = eventNameAndVersionToTypeMap.get(new NameAndVersion(eventName, eventVersion));
        if (eventType == null) {
            throw new EventTypeMappingException("Mapping to event type not found for event name: " + eventName + " and event version: " + eventVersion);
        }
        return eventType;
    }

    @Override
    public void registerMapping(TypeNameAndVersion<? extends E> mapping) {
        validateUniqueEventNameAndVersion(mapping);
        validateUniqueType(mapping);

        eventNameAndVersionToTypeMap.put(mapping.getNameAndVersion(), mapping.getType());
        eventTypeToNameAndVersionMap.put(mapping.getType(), mapping.getNameAndVersion());

    }

    /**
     * Validates if user did not configure same event name and version for more than one type, for example:
     * eventTypeMapper.registerMapping(OrderPlaced.class, "OrderPlaced", 1);
     * eventTypeMapper.registerMapping(OrderCancelled.class, "OrderPlaced", 1);
     */
    private void validateUniqueEventNameAndVersion(TypeNameAndVersion<? extends E> mapping) {
        if (eventNameAndVersionToTypeMap.containsKey(mapping.getNameAndVersion())) {
            Class<? extends E> type = eventNameAndVersionToTypeMap.get(mapping.getNameAndVersion());
            throw new NonUniqueMappingException(
                    String.format("Mapping for event name: %s and version: %s was already configured for type: %s",
                            mapping.getName(), mapping.getVersion(), type
                    )
            );
        }
    }

    /**
     * Validates if user did not configure same event type more than once, for example:
     * eventTypeMapper.registerMapping(OrderPlaced.class, "OrderPlaced", 1);
     * eventTypeMapper.registerMapping(OrderPlaced.class, "OrderPlacedNew", 2);
     */
    private void validateUniqueType(TypeNameAndVersion<? extends E> mapping) {
        if (eventTypeToNameAndVersionMap.containsKey(mapping.getType())) {
            NameAndVersion nameAndVersion = eventTypeToNameAndVersionMap.get(mapping.getType());
            throw new NonUniqueMappingException(
                    String.format("Mapping for event type: %s was already configured for event name: %s and version: %s",
                            mapping.getType(), nameAndVersion.getName(), nameAndVersion.getVersion()
                    )
            );
        }
    }

}
