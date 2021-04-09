package com.hltech.store.versioning;

public class UpcastingEventVersionPolicy<E> implements EventVersionPolicy<E> {

    @Override
    public String toName(Class<? extends E> eventType) {
        throw new IllegalStateException("Not yet implemented");
    }

    @Override
    public int toVersion(Class<? extends E> eventType) {
        throw new IllegalStateException("Not yet implemented");
    }

    @Override
    public Class<? extends E> toType(String eventName, int eventVersion) {
        throw new IllegalStateException("Not yet implemented");
    }

}
