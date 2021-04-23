package com.hltech.store.versioning;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

/**
 * In this strategy multiple versions of the event are wrapped, using registered wrapper.
 * Wrapper is a class that extends desirable event type and provides all data directly from wrapped json.
 *
 * <p>This strategy is recommended when you have a multiple instance of your application running at the same time
 * because it supports backward and forward compatibility.
 *
 * <p>In opposite to {@link MappingBasedVersioning}, at the expense of more code
 * it allows to rename events attributes without the need to use the copy and replace mechanism.
 */
public class WrappingBasedVersioning<E> implements EventVersioningStrategy<E> {

    private static final int CONSTANT_VERSION_NUMBER = 1;

    private final Map<Class<? extends E>, String> eventTypeToNameMap = new HashMap<>();
    private final Map<String, Function<String, E>> eventNameToWrapperMap = new HashMap<>();

    @Getter
    private final ObjectMapper objectMapper;

    public WrappingBasedVersioning() {
        objectMapper = new ObjectMapper();
        objectMapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(FAIL_ON_MISSING_CREATOR_PROPERTIES, false);
    }

    @Override
    public E toEvent(String eventJson, String eventName, int eventVersion) {
        Function<String, E> wrapper = eventNameToWrapperMap.get(eventName);
        try {
            return wrapper.apply(eventJson);
        } catch (Exception ex) {
            throw new EventBodyMappingException(eventJson, eventName, ex);
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

    public void registerEvent(
            Class<? extends E> eventType,
            String eventName,
            Function<String, E> wrapper
    ) {
        eventTypeToNameMap.put(eventType, eventName);
        eventNameToWrapperMap.put(eventName, wrapper);
    }

}
