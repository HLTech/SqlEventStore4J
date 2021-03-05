package com.hltech.store;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public interface EventStore {

    void save(
            Event event,
            String streamType
    );

    List<Event> findAll(UUID aggregateId);

    List<Event> findAll(String streamType);

    <T> Optional<T> findAggregate(
            UUID aggregateId,
            String streamType,
            Supplier<T> initialAggregateStateSupplier,
            BiFunction<T, Event, T> eventApplier
    );

    <T> T getAggregate(
            UUID aggregateId,
            String streamType,
            Supplier<T> initialAggregateStateSupplier,
            BiFunction<T, Event, T> eventApplier
    );

    <T> Optional<T> findAggregateToEvent(
            Event toEvent,
            String streamType,
            Supplier<T> initialAggregateStateSupplier,
            BiFunction<T, Event, T> eventApplier
    );

    <T> T getAggregateToEvent(
            Event toEvent,
            String streamType,
            Supplier<T> initialAggregateStateSupplier,
            BiFunction<T, Event, T> eventApplier
    );

    <T> List<T> findAllAggregate(
            String streamType,
            Supplier<T> initialAggregateStateSupplier,
            BiFunction<T, Event, T> eventApplier
    );

}
