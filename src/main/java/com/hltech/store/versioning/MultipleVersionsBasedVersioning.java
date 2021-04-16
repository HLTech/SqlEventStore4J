package com.hltech.store.versioning;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

/**
 * In this strategy multiple versions of the event have to be supported in the application code.
 * The application must contain knowledge of all deprecated event versions in order to support them.
 * To avoid that consider using {@link UpcastingBasedVersioning}.
 *
 * <p>Please note that using this strategy is recommended only if you have one instance of your application running at the same time.
 * Using this strategy in multi instance case leads to the situation where all instances must be updated
 * to understand latest event version before any instance produces it. For multi instance case consider using {@link MappingBasedVersioning}
 */
public class MultipleVersionsBasedVersioning<E> implements EventVersioningStrategy<E> {

    private final Map<NameAndVersion, Class<? extends E>> eventNameAndVersionToTypeMap = new HashMap<>();
    private final Map<Class<? extends E>, NameAndVersion> eventTypeToNameAndVersionMap = new HashMap<>();

    @Getter
    private final ObjectMapper objectMapper;

    public MultipleVersionsBasedVersioning() {
        objectMapper = new ObjectMapper();
        objectMapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, true);
        objectMapper.configure(FAIL_ON_MISSING_CREATOR_PROPERTIES, true);
    }

    @Override
    public E toEvent(String eventJson, String eventName, int eventVersion) {
        Class<? extends E> eventType = toType(eventName, eventVersion);
        try {
            return objectMapper.readValue(eventJson, eventType);
        } catch (Exception ex) {
            throw new EventBodyMappingException(eventJson, eventType, ex);
        }
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
    public String toJson(E event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception ex) {
            throw new EventBodyMappingException(event, ex);
        }
    }

    public void registerEvent(Class<? extends E> eventType, String eventName, int eventVersion) {
        NameAndVersion nameAndVersion = new NameAndVersion(eventName, eventVersion);
        validateUniqueEventNameAndVersion(nameAndVersion);
        validateUniqueType(eventType);
        eventNameAndVersionToTypeMap.put(nameAndVersion, eventType);
        eventTypeToNameAndVersionMap.put(eventType, nameAndVersion);
    }

    private Class<? extends E> toType(String eventName, int eventVersion) {
        Class<? extends E> eventType = eventNameAndVersionToTypeMap.get(new NameAndVersion(eventName, eventVersion));
        if (eventType == null) {
            throw new EventTypeMappingException("Mapping to event type not found for event name: " + eventName + " and event version: " + eventVersion);
        }
        return eventType;
    }

    /**
     * Validates if user did not configure same event name and version for more than one type, for example:
     * eventTypeMapper.registerMapping(OrderPlaced.class, "OrderPlaced", 1);
     * eventTypeMapper.registerMapping(OrderCancelled.class, "OrderPlaced", 1);
     */
    private void validateUniqueEventNameAndVersion(NameAndVersion nameAndVersion) {
        if (eventNameAndVersionToTypeMap.containsKey(nameAndVersion)) {
            Class<? extends E> type = eventNameAndVersionToTypeMap.get(nameAndVersion);
            throw new NonUniqueMappingException(
                    String.format("Mapping for event name: %s and version: %s was already configured for type: %s",
                            nameAndVersion.getName(), nameAndVersion.getVersion(), type
                    )
            );
        }
    }

    /**
     * Validates if user did not configure same event type more than once, for example:
     * eventTypeMapper.registerMapping(OrderPlaced.class, "OrderPlaced", 1);
     * eventTypeMapper.registerMapping(OrderPlaced.class, "OrderPlacedNew", 2);
     */
    private void validateUniqueType(Class<? extends E> eventType) {
        if (eventTypeToNameAndVersionMap.containsKey(eventType)) {
            NameAndVersion nameAndVersion = eventTypeToNameAndVersionMap.get(eventType);
            throw new NonUniqueMappingException(
                    String.format("Mapping for event type: %s was already configured for event name: %s and version: %s",
                            eventType, nameAndVersion.getName(), nameAndVersion.getVersion()
                    )
            );
        }
    }

}
