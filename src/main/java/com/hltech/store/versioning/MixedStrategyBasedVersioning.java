package com.hltech.store.versioning;

/**
 * This strategy allows to use different event versioning strategy for each event. Available policies are:
 * {@link MultipleVersionsBasedVersioning}
 * {@link UpcastingBasedVersioning}
 * {@link MappingBasedVersioning}
 * {@link WrappingBasedVersioning}
 */
public class MixedStrategyBasedVersioning<E> implements EventVersioningStrategy<E> {

    @Override
    public E toEvent(String eventJson, String eventName, int eventVersion) {
        throw new IllegalStateException("Not yet implemented");
    }

    @Override
    public String toName(Class<? extends E> eventType) {
        throw new IllegalStateException("Not yet implemented");
    }

    @Override
    public int toVersion(Class<? extends E> eventType) {
        throw new IllegalStateException("Not yet implemented");
    }

    @Override
    public String toJson(E event) {
        throw new IllegalStateException("Not yet implemented");
    }

}
