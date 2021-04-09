package com.hltech.store.versioning;

import java.util.HashMap;
import java.util.Map;

/**
 * In this policy, multiple versions of the event has to bo supported in the application code.
 * The application must contain knowledge of all deprecated event versions in order to support them.
 * To avoid that consider using {@link UpcastingEventVersionPolicy}
 *
 * <p>Please note, that using this policy is recommended only if you have one instance of you application running at the same time.
 * Using this policy in multi instance case, leads to the situation, where all instance must be updated
 * to understand latest event version, before any instance produce it. For multi instance case consider using {@link NoEventVersionPolicy}
 */
public class MultipleEventVersionPolicy<E> implements EventVersionPolicy<E> {

    private final Map<NameAndVersion, Class<? extends E>> eventNameAndVersionToTypeMap = new HashMap<>();
    private final Map<Class<? extends E>, NameAndVersion> eventTypeToNameAndVersionMap = new HashMap<>();

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

    public void registerMapping(Class<? extends E> eventType, String eventName, int eventVersion) {
        NameAndVersion nameAndVersion = new NameAndVersion(eventName, eventVersion);
        validateUniqueEventNameAndVersion(nameAndVersion);
        validateUniqueType(eventType);
        eventNameAndVersionToTypeMap.put(nameAndVersion, eventType);
        eventTypeToNameAndVersionMap.put(eventType, nameAndVersion);
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
