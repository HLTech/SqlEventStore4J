package com.hltech.store.versioning;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public interface EventVersioningStrategy<E> {

    E toEvent(String eventJson, String eventName, int eventVersion);

    String toName(Class<? extends E> eventType);

    int toVersion(Class<? extends E> eventType);

    String toJson(E event);

    @EqualsAndHashCode
    @RequiredArgsConstructor
    @Getter
    class NameAndVersion {

        private final String name;
        private final int version;

    }

}
