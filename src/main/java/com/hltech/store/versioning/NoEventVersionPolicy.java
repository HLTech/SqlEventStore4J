package com.hltech.store.versioning;

import java.util.HashMap;
import java.util.Map;

/**
 * In this policy, every event exists only in latest version, so that the application code has to support only one version of the event.
 * It is also recommended when you have multiple instance of you application running at the same time, because it supports backward and forward compatibility.
 */
public class NoEventVersionPolicy<E> implements EventVersionPolicy<E> {

    private static final int CONSTANT_VERSION_NUMBER = 1;

    private final Map<String, Class<? extends E>> eventNameToTypeMap = new HashMap<>();
    private final Map<Class<? extends E>, String> eventTypeToNameMap = new HashMap<>();

    @Override
    public String toName(Class<? extends E> eventType) {
        String eventName = eventTypeToNameMap.get(eventType);
        if (eventName == null) {
            throw new EventTypeMappingException("Mapping to event name not found for event type: " + eventType);
        }
        return eventName;
    }

    @Override
    public int toVersion(Class<? extends E> eventType) {
        return CONSTANT_VERSION_NUMBER;
    }

    @Override
    public Class<? extends E> toType(String eventName, int eventVersion) {
        Class<? extends E> eventType = eventNameToTypeMap.get(eventName);
        if (eventType == null) {
            throw new EventTypeMappingException("Mapping to event type not found for event name: " + eventName);
        }
        return eventType;
    }

    public void registerMapping(Class<? extends E> eventType, String eventName) {
        validateUniqueEventName(eventName);
        validateUniqueType(eventType);
        eventNameToTypeMap.put(eventName, eventType);
        eventTypeToNameMap.put(eventType, eventName);
    }

    /**
     * Validates if user did not configure same event name and version for more than one type, for example:
     * eventTypeMapper.registerMapping(OrderPlaced.class, "OrderPlaced");
     * eventTypeMapper.registerMapping(OrderCancelled.class, "OrderPlaced");
     */
    private void validateUniqueEventName(String eventName) {
        if (eventNameToTypeMap.containsKey(eventName)) {
            Class<? extends E> type = eventNameToTypeMap.get(eventName);
            throw new NonUniqueMappingException(
                    String.format("Mapping for event name: %s was already configured for type: %s", eventName, type)
            );
        }
    }

    /**
     * Validates if user did not configure same event type more than once, for example:
     * eventTypeMapper.registerMapping(OrderPlaced.class, "OrderPlaced");
     * eventTypeMapper.registerMapping(OrderPlaced.class, "OrderPlacedNew");
     */
    private void validateUniqueType(Class<? extends E> eventType) {
        if (eventTypeToNameMap.containsKey(eventType)) {
            String eventName = eventTypeToNameMap.get(eventType);
            throw new NonUniqueMappingException(
                    String.format("Mapping for event type: %s was already configured for event name: %s", eventType, eventName)
            );
        }
    }

}
