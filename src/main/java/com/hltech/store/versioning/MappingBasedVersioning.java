package com.hltech.store.versioning;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * In this strategy every event exists only in latest version so that the application code has to support only one version of the event.
 * The mapping strategy is based on three simple principles:
 * - When attribute exists on both json and class then set the value from json
 * - When attribute exists on json but not on class then do nothing
 * - When attribute exists on class but not in json then set default value
 *
 * <p>This strategy is recommended when you have a multiple instance of your application running at the same time
 * because it supports backward and forward compatibility.
 *
 * <p>Be aware that it also has one important and annoying drawback. You are no longer allowed to rename event attribute.
 * What you can do when attribute name is no longer valid, is:
 * - add new attribute with valid name and support both attributes
 * - use copy and replace mechanism to fix no longer valid attribute name
 * - use {@link WrappingBasedVersioning} instead
 */
public class MappingBasedVersioning<E> implements EventVersioningStrategy<E> {

    private static final int CONSTANT_VERSION_NUMBER = 1;

    private final Map<String, Class<? extends E>> eventNameToTypeMap = new HashMap<>();
    private final Map<Class<? extends E>, String> eventTypeToNameMap = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public E toEvent(String eventJson, String eventName, int eventVersion) {
        Class<? extends E> eventType = toType(eventName);
        try {
            return objectMapper.readValue(eventJson, eventType);
        } catch (Exception ex) {
            throw new EventBodyMappingException(eventJson, eventType, ex);
        }
    }

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
    public String toJson(E event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception ex) {
            throw new EventBodyMappingException(event, ex);
        }
    }

    public void registerMapping(Class<? extends E> eventType, String eventName) {
        validateUniqueEventName(eventName);
        validateUniqueType(eventType);
        eventNameToTypeMap.put(eventName, eventType);
        eventTypeToNameMap.put(eventType, eventName);
    }

    private Class<? extends E> toType(String eventName) {
        Class<? extends E> eventType = eventNameToTypeMap.get(eventName);
        if (eventType == null) {
            throw new EventTypeMappingException("Mapping to event type not found for event name: " + eventName);
        }
        return eventType;
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
