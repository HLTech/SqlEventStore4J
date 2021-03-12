package com.hltech.store;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toList;

public class AggregateRepository<A, E> {

    private final EventStore<E> eventStore;
    private final Supplier<A> initialAggregateStateSupplier;
    private final BiFunction<A, E, A> eventApplier;
    private final String streamType;

    public AggregateRepository(
            EventStore<E> eventStore,
            Supplier<A> initialAggregateStateSupplier,
            BiFunction<A, E, A> eventApplier,
            String streamType
    ) {
        this.eventStore = eventStore;
        this.initialAggregateStateSupplier = initialAggregateStateSupplier;
        this.eventApplier = eventApplier;
        this.streamType = streamType;
    }

    public void save(E event) {
        eventStore.save(event, streamType);
    }

    public Optional<A> find(
            UUID aggregateId
    ) {
        List<E> events = eventStore.findAll(aggregateId, streamType);
        return eventsToAggregate(events);
    }

    public A get(UUID aggregateId) {
        return find(aggregateId)
                .orElseThrow(() -> new AggregateRepositoryException("Could not find aggregate with id: " + aggregateId + " in stream: " + streamType));
    }

    public Optional<A> findToEvent(E toEvent) {
        List<E> events = eventStore.findAllToEvent(toEvent, streamType);
        return eventsToAggregate(events);
    }

    public A getToEvent(E toEvent) {
        return findToEvent(toEvent)
                .orElseThrow(() -> new AggregateRepositoryException("Could not find aggregate to event: " + toEvent + " in stream: " + streamType));
    }

    public List<A> findAll(String streamType) {
        return eventStore
                .findAll(streamType)
                .values()
                .stream()
                .map(this::eventsToAggregate)
                .map(Optional::get)
                .collect(toList());
    }

    private Optional<A> eventsToAggregate(List<E> events) {
        if (events.isEmpty()) {
            return Optional.empty();
        } else {

            A aggregate = initialAggregateStateSupplier.get();
            for (E event : events) {
                aggregate = eventApplier.apply(aggregate, event);
            }

            return Optional.of(aggregate);
        }
    }

}

