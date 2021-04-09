package com.hltech.store.versioning;

/**
 * In this policy, multiple versions of the event are up-casted to the latest version, using registered transformation.
 * In opposite to {@link MultipleEventVersionPolicy} the application code has to support only the latest version of the event
 *
 * <p>Please note, that using this policy is recommended only if you have one instance of you application running at the same time.
 * Using this policy in multi instance case, leads to the situation, where all instance must be updated
 * to understand latest event version, before any instance produce it. For multi instance case consider using {@link NoEventVersionPolicy}
 */
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
