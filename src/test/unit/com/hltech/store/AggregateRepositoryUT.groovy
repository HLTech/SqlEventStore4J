package com.hltech.store

import spock.lang.Specification
import spock.lang.Subject

class AggregateRepositoryUT extends Specification {

    def eventStore = Mock(EventStore)

    @Subject
    AggregateRepository<DummyAggregate, DummyBaseEvent> repository = new AggregateRepository(
            eventStore,
            AGGREGATE_NAME,
            DummyAggregate.INITIAL_STATE_SUPPLIER,
            DummyAggregate.EVENT_APPLIER,
            DummyAggregate.VERSION_APPLIER
    )

    def "save should save event in event store in a proper stream"() {

        when: 'Save event'
            repository.save(EVENT)

        then: 'Event saved in proper stream'
            1 * eventStore.save(EVENT, AGGREGATE_NAME)

    }

    def "save with optimistic lock should save event in event store in a proper stream"() {

        given: 'Expected aggregate version'
            Integer expectedAggregateVersion = 1

        when: 'Save event'
            repository.save(EVENT, expectedAggregateVersion)

        then: 'Event saved in proper stream'
            1 * eventStore.save(EVENT, AGGREGATE_NAME, expectedAggregateVersion)

    }

    def "find should return aggregate with all events applied"() {

        given: 'Events for aggregate exists in event store'
            eventStore.findAll(AGGREGATE_ID, AGGREGATE_NAME) >> [EVENT]

        when: 'Search for aggregate'
            Optional<DummyAggregate> aggregate = repository.find(AGGREGATE_ID)

        then: 'Aggregate found'
            aggregate.isPresent()

        and: 'Events applied'
            aggregate.get().appliedEvents == [EVENT]

        and: 'Version applied'
            aggregate.get().version == 1

    }

    def "find should not return aggregate when there is no events for aggregate in event store"() {

        given: 'Events for aggregate does not exist in event store'
            eventStore.findAll(AGGREGATE_ID, AGGREGATE_NAME) >> []

        when: 'Search for aggregate'
            Optional<DummyAggregate> aggregate = repository.find(AGGREGATE_ID)

        then: 'Aggregate not found'
            aggregate.isEmpty()

    }

    def "get should return aggregate with all events applied"() {

        given: 'Events for aggregate exists in event store'
            eventStore.findAll(AGGREGATE_ID, AGGREGATE_NAME) >> [EVENT]

        when: 'Get aggregate'
            DummyAggregate aggregate = repository.get(AGGREGATE_ID)

        then: 'Aggregate exist'
            aggregate != null

        and: 'Events applied'
            aggregate.appliedEvents == [EVENT]

        and: 'Version applied'
            aggregate.version == 1

    }

    def "get should throw exception when there is no events for aggregate in event store"() {

        given: 'Events for aggregate does not exist in event store'
            eventStore.findAll(AGGREGATE_ID, AGGREGATE_NAME) >> []

        when: 'Get aggregate'
            repository.get(AGGREGATE_ID)

        then: 'Exception thrown'
            def ex = thrown(AggregateRepositoryException)
            ex.message == "Could not find aggregate with id: $AGGREGATE_ID and name: $AGGREGATE_NAME"

    }

    def "findToEvent should return aggregate with all events applied"() {

        given: 'Events for aggregate exists in event store'
            eventStore.findAllToEvent(EVENT, AGGREGATE_NAME) >> [EVENT]

        when: 'Search for aggregate to event'
            Optional<DummyAggregate> aggregate = repository.findToEvent(EVENT)

        then: 'Aggregate found'
            aggregate.isPresent()

        and: 'Events applied'
            aggregate.get().appliedEvents == [EVENT]

        and: 'Version applied'
            aggregate.get().version == 1

    }

    def "findToEvent should not return aggregate when there is no events for aggregate in event store"() {

        given: 'Events for aggregate does not exist in event store'
            eventStore.findAllToEvent(EVENT, AGGREGATE_NAME) >> []

        when: 'Search for aggregate to event'
            Optional<DummyAggregate> aggregate = repository.findToEvent(EVENT)

        then: 'Aggregate not found'
            aggregate.isEmpty()

    }

    def "getToEvent should return aggregate with all events applied"() {

        given: 'Events for aggregate exists in event store'
            eventStore.findAllToEvent(EVENT, AGGREGATE_NAME) >> [EVENT]

        when: 'Get aggregate to event'
            DummyAggregate aggregate = repository.getToEvent(EVENT)

        then: 'Aggregate exist'
            aggregate != null

        and: 'Events applied'
            aggregate.appliedEvents == [EVENT]

        and: 'Version applied'
            aggregate.version == 1

    }

    def "getToEvent should throw exception when there is no events for aggregate in event store"() {

        given: 'Events for aggregate does not exist in event store'
            eventStore.findAllToEvent(EVENT, AGGREGATE_NAME) >> []

        when: 'Get aggregate to event'
            repository.getToEvent(EVENT)

        then: 'Exception thrown'
            def ex = thrown(AggregateRepositoryException)
            ex.message == "Could not find aggregate to event: $EVENT for aggregate name: $AGGREGATE_NAME"

    }

    def "findAllGroupByAggregate should return all aggregates with name"() {

        given: 'Events for aggregate exists in event store for aggregate name'
            eventStore.findAllGroupByAggregate(AGGREGATE_NAME) >> [AGGREGATE_ID: [EVENT] ]

        when: 'Search for aggregates'
            List<DummyAggregate> aggregates = repository.findAll()

        then: 'Aggregates found'
            aggregates.size() == 1

        and: 'Events applied'
            aggregates[0].appliedEvents == [EVENT]

        and: 'Version applied'
            aggregates[0].version == 1

    }

    def "findAllGroupByAggregate should return empty list when there is no events related to aggregate name"() {

        given: 'Events does not exists in event store for aggregate name'
            eventStore.findAllGroupByAggregate(AGGREGATE_NAME) >> [:]

        when: 'Search for aggregates'
            List<DummyAggregate> aggregates = repository.findAll()

        then: 'Aggregates not found'
            aggregates.empty

    }

    def "findAllEvents should return all events related to aggregate name"() {

        given: 'Events for aggregate exists in event store for aggregate name'
            eventStore.findAll(AGGREGATE_NAME) >> [EVENT]

        when: 'Search for events'
            List<DummyAggregate> events = repository.findAllEvents()

        then: 'Events found'
            events == [EVENT]

    }

    def "contains should return true when event exist in event store "() {

        given: 'Event exist in event store'
            eventStore.contains(EVENT, AGGREGATE_NAME) >> true

        expect: 'Contains return true'
            repository.contains(EVENT)

    }


    def "contains should return false when event does not exist in event store"() {

        given: 'Event does not exist in event store'
            eventStore.contains(EVENT, AGGREGATE_NAME) >> false

        expect: 'Contains return false'
            !repository.contains(EVENT)

    }

    static AGGREGATE_NAME = "DummyStream"
    static AGGREGATE_ID = UUID.randomUUID()
    static EVENT = new DummyEvent(AGGREGATE_ID)

}
