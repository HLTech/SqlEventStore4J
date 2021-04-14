package com.hltech.store


import com.hltech.store.versioning.EventVersioningStrategy
import com.hltech.store.versioning.MultipleVersionsBasedVersioning
import spock.lang.Specification
import spock.lang.Subject

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric


class AggregateRepositoryFT extends Specification implements PostgreSQLContainerTest {

    EventVersioningStrategy<DummyBaseEvent> eventVersioningStrategy = new MultipleVersionsBasedVersioning<>()
    EventStore<DummyBaseEvent> eventStore = new PostgresEventStore(
            DummyBaseEvent.EVENT_ID_EXTRACTOR,
            DummyBaseEvent.AGGREGATE_ID_EXTRACTOR,
            eventVersioningStrategy,
            dataSource
    )

    @Subject
    def repository = new AggregateRepository<DummyAggregate, DummyBaseEvent>(
            eventStore,
            AGGREGATE_NAME,
            DummyAggregate.INITIAL_STATE_SUPPLIER,
            DummyAggregate.EVENT_APPLIER,
            DummyAggregate.VERSION_APPLIER
    )

    def "find should return aggregate with events and version applied"() {

        given: 'Events types mapping registered'
            eventVersioningStrategy.registerMapping(DummyEvent, "DummyEvent", 1)
            eventVersioningStrategy.registerMapping(AnotherDummyEvent, "AnotherDummyEvent", 1)

        and: 'Events saved in repository'
            UUID aggregateId = UUID.randomUUID()
            def dummyEvent = new DummyEvent(aggregateId)
            repository.save(dummyEvent)
            def anotherDummyEvent = new AnotherDummyEvent(aggregateId)
            repository.save(anotherDummyEvent)

        when: 'search for aggregate'
            Optional<DummyAggregate> aggregate = repository.find(aggregateId)

        then: 'aggregate found'
            aggregate.isPresent()

        and: 'Events applied'
            aggregate.get().appliedEvents == [dummyEvent, anotherDummyEvent]

        and: 'Version applied'
            aggregate.get().version == 2

    }

    static AGGREGATE_NAME = randomAlphanumeric(5)

}
