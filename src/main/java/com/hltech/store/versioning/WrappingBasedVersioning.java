package com.hltech.store.versioning;

/**
 * In this strategy multiple versions of the event are wrapped, using registered wrapper.
 * Wrapper is a class that extends desirable event type and provides all data directly from wrapped json.
 *
 * <p>This strategy is recommended when you have a multiple instance of your application running at the same time
 * because it supports backward and forward compatibility.
 *
 * <p>In opposite to {@link MappingBasedVersioning}, at the expense of more code
 * it allows to rename events attributes without the need to use the copy & replace mechanism.
 */
public class WrappingBasedVersioning<E> implements EventVersioningStrategy<E> {

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
