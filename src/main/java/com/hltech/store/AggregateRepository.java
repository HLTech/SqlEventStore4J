package com.hltech.store;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toList;

public class AggregateRepository<A, E> {

    private final EventStore<E> eventStore;
    private final String aggregateName;
    private final Supplier<A> initialAggregateStateSupplier;
    private final BiFunction<A, E, A> eventApplier;
    private final BiFunction<A, Integer, A> aggregateVersionApplier;

    public AggregateRepository(
            EventStore<E> eventStore,
            String aggregateName,
            Supplier<A> initialAggregateStateSupplier,
            BiFunction<A, E, A> eventApplier,
            BiFunction<A, Integer, A> aggregateVersionApplier
    ) {
        this.eventStore = eventStore;
        this.initialAggregateStateSupplier = initialAggregateStateSupplier;
        this.eventApplier = eventApplier;
        this.aggregateName = aggregateName;
        this.aggregateVersionApplier = aggregateVersionApplier;
    }

    public AggregateRepository(
            EventStore<E> eventStore,
            String aggregateName,
            Supplier<A> initialAggregateStateSupplier,
            BiFunction<A, E, A> eventApplier
    ) {
        this(
                eventStore,
                aggregateName,
                initialAggregateStateSupplier,
                eventApplier,
                (aggregate, version) -> aggregate
        );
    }

    public void save(E event) {
        eventStore.save(event, aggregateName);
    }

    public void save(E event, int expectedAggregateVersion) {
        eventStore.save(event, aggregateName, expectedAggregateVersion);
    }

    public Optional<A> find(
            UUID aggregateId
    ) {
        List<E> events = eventStore.findAll(aggregateId, aggregateName);
        return toAggregate(events);
    }

    public A get(UUID aggregateId) {
        return find(aggregateId)
                .orElseThrow(() -> new AggregateRepositoryException("Could not find aggregate with id: " + aggregateId + " and name: " + aggregateName));
    }

    public Optional<A> findToEvent(E toEvent) {
        List<E> events = eventStore.findAllToEvent(toEvent, aggregateName);
        return toAggregate(events);
    }

    public A getToEvent(E toEvent) {
        return findToEvent(toEvent)
                .orElseThrow(() -> new AggregateRepositoryException("Could not find aggregate to event: " + toEvent + " for aggregate name: " + aggregateName));
    }

    public List<A> findAll() {
        return eventStore
                .findAllGroupByAggregate(aggregateName)
                .values()
                .stream()
                .map(this::toAggregate)
                .map(Optional::get)
                .collect(toList());
    }

    public List<E> findAllEvents() {
        return eventStore.findAll(aggregateName);
    }

    public boolean contains(E event) {
        return eventStore.contains(event, aggregateName);
    }

    private Optional<A> toAggregate(List<E> events) {
        if (events.isEmpty()) {
            return Optional.empty();
        } else {

            A aggregate = initialAggregateStateSupplier.get();
            for (E event : events) {
                aggregate = eventApplier.apply(aggregate, event);
            }
            aggregate = aggregateVersionApplier.apply(aggregate, events.size());
            return Optional.of(aggregate);
        }
    }

}
