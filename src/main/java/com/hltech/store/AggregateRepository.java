package com.hltech.store;

import io.vavr.collection.Iterator;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static io.vavr.collection.Stream.ofAll;
import static java.util.stream.Collectors.toList;

public class AggregateRepository<T> {

    private final EventStore eventStore;
    private final Supplier<T> initialAggregateStateSupplier;
    private final BiFunction<T, Event, T> eventApplier;
    private final String streamType;

    public AggregateRepository(
            EventStore eventStore,
            Supplier<T> initialAggregateStateSupplier,
            BiFunction<T, Event, T> eventApplier,
            String streamType
    ) {
        this.eventStore = eventStore;
        this.initialAggregateStateSupplier = initialAggregateStateSupplier;
        this.eventApplier = eventApplier;
        this.streamType = streamType;
    }

    public void save(Event event) {
        eventStore.save(event, streamType);
    }

    public Optional<T> find(
            UUID aggregateId
    ) {
        List<Event> events = eventStore.findAll(aggregateId, streamType);
        return eventsToAggregate(events);
    }

    public T get(UUID aggregateId) {
        return find(aggregateId)
                .orElseThrow(() -> new IllegalStateException("Could not find aggregate with id: " + aggregateId + " in stream: " + streamType));
    }

    public Optional<T> findToEvent(Event toEvent) {
        List<Event> events = eventStore.findAllToEvent(toEvent, streamType);
        return eventsToAggregate(events);
    }

    public T getToEvent(Event toEvent) {
        return findToEvent(toEvent)
                .orElseThrow(() -> new IllegalStateException("Could not find aggregate to event with id: " + toEvent.getId() + " in stream: " + streamType));
    }

    public List<T> findAll(String streamType) {
        List<Event> events = eventStore.findAll(streamType);
        return Iterator.ofAll(events)
                .groupBy(Event::getAggregateId)
                .mapValues(releaseCandidateGroupEvents -> eventsToAggregate(releaseCandidateGroupEvents.toJavaList()))
                .values()
                .map(Optional::get)
                .collect(toList());
    }

    private Optional<T> eventsToAggregate(List<Event> events) {
        if (events.isEmpty()) {
            return Optional.empty();
        } else {
            T aggregate = ofAll(events).foldLeft(initialAggregateStateSupplier.get(), eventApplier);
            return Optional.of(aggregate);
        }
    }

}

