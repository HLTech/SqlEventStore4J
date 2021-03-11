package com.hltech.store

import spock.lang.Specification
import spock.lang.Subject

class AggregateRepositoryUT extends Specification {

    def eventStore = Mock(EventStore)

    @Subject
    AggregateRepository<DummyAggregate, DummyBaseEvent> repository = new AggregateRepository(
            eventStore,
            DummyAggregate.INITIAL_STATE_SUPPLIER,
            DummyAggregate.EVENT_APPLIER,
            STREAM_TYPE
    )

    def "save should save event in event store in a proper stream"() {

        when: 'Save event'
            repository.save(EVENT)

        then: 'Event saved in proper stream'
            1 * eventStore.save(EVENT, STREAM_TYPE)

    }

    def "find should return aggregate with all events applied"() {

        given: 'Events for aggregate exists in event store'
            eventStore.findAll(AGGREGATE_ID, STREAM_TYPE) >> [EVENT]

        when: 'Search for aggregate'
            Optional<DummyAggregate> aggregate = repository.find(AGGREGATE_ID)

        then: 'Aggregate found'
            aggregate.isPresent()

        and: 'Events applied'
            aggregate.get().appliedEvents == [EVENT]

    }

    def "find should not return aggregate when there is no events for aggregate in event store"() {

        given: 'Events for aggregate does not exist in event store'
            eventStore.findAll(AGGREGATE_ID, STREAM_TYPE) >> []

        when: 'Search for aggregate'
            Optional<DummyAggregate> aggregate = repository.find(AGGREGATE_ID)

        then: 'Aggregate not found'
            aggregate.isEmpty()

    }

    def "get should return aggregate with all events applied"() {

        given: 'Events for aggregate exists in event store'
            eventStore.findAll(AGGREGATE_ID, STREAM_TYPE) >> [EVENT]

        when: 'Get aggregate'
            DummyAggregate aggregate = repository.get(AGGREGATE_ID)

        then: 'Aggregate exist'
            aggregate != null

        and: 'Events applied'
            aggregate.appliedEvents == [EVENT]

    }

    def "get should throw exception when there is no events for aggregate in event store"() {

        given: 'Events for aggregate does not exist in event store'
            eventStore.findAll(AGGREGATE_ID, STREAM_TYPE) >> []

        when: 'Get aggregate'
            repository.get(AGGREGATE_ID)

        then: 'Exception thrown'
            def ex = thrown(IllegalStateException)
            ex.message == "Could not find aggregate with id: $AGGREGATE_ID in stream: $STREAM_TYPE"

    }

    def "findToEvent should return aggregate with all events applied"() {

        given: 'Events for aggregate exists in event store'
            eventStore.findAllToEvent(EVENT, STREAM_TYPE) >> [EVENT]

        when: 'Search for aggregate to event'
            Optional<DummyAggregate> aggregate = repository.findToEvent(EVENT)

        then: 'Aggregate found'
            aggregate.isPresent()

        and: 'Events applied'
            aggregate.get().appliedEvents == [EVENT]

    }

    def "findToEvent should not return aggregate when there is no events for aggregate in event store"() {

        given: 'Events for aggregate does not exist in event store'
            eventStore.findAllToEvent(EVENT, STREAM_TYPE) >> []

        when: 'Search for aggregate to event'
            Optional<DummyAggregate> aggregate = repository.findToEvent(EVENT)

        then: 'Aggregate not found'
            aggregate.isEmpty()

    }

    def "getToEvent should return aggregate with all events applied"() {

        given: 'Events for aggregate exists in event store'
            eventStore.findAllToEvent(EVENT, STREAM_TYPE) >> [EVENT]

        when: 'Get aggregate to event'
            DummyAggregate aggregate = repository.getToEvent(EVENT)

        then: 'Aggregate exist'
            aggregate != null

        and: 'Events applied'
            aggregate.appliedEvents == [EVENT]

    }

    def "getToEvent should throw exception when there is no events for aggregate in event store"() {

        given: 'Events for aggregate does not exist in event store'
            eventStore.findAllToEvent(EVENT, STREAM_TYPE) >> []

        when: 'Get aggregate to event'
            repository.getToEvent(EVENT)

        then: 'Exception thrown'
            def ex = thrown(IllegalStateException)
            ex.message == "Could not find aggregate to event: $EVENT in stream: $STREAM_TYPE"

    }

    def "findAll should return all aggregates within stream"() {

        given: 'Events for aggregate exists in event store for stream'
            eventStore.findAll(STREAM_TYPE) >> [ AGGREGATE_ID: [EVENT] ]

        when: 'Search for aggregates'
            List<DummyAggregate> aggregates = repository.findAll(STREAM_TYPE)

        then: 'Aggregates found'
            aggregates.size() == 1

        and: 'Events applied'
            aggregates[0].appliedEvents == [EVENT]

    }

    def "findAll should return empty list when there is no events in the stream"() {

        given: 'Events does not exists in event store for stream'
            eventStore.findAll(STREAM_TYPE) >> [:]

        when: 'Search for aggregates'
            List<DummyAggregate> aggregates = repository.findAll(STREAM_TYPE)

        then: 'Aggregates not found'
            aggregates.empty

    }

    static STREAM_TYPE = "DummyStream"
    static AGGREGATE_ID = UUID.randomUUID()
    static EVENT = new DummyEvent(AGGREGATE_ID)

}
