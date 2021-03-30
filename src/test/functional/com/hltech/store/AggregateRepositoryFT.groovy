package com.hltech.store

import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Specification
import spock.lang.Subject

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric

class AggregateRepositoryFT extends Specification implements PostgreSQLContainerTest {

    EventTypeMapper<DummyBaseEvent> eventTypeMapper = new SimpleEventTypeMapper<>()
    EventBodyMapper<DummyBaseEvent> eventBodyMapper = new JacksonEventBodyMapper<>(new ObjectMapper())
    EventStore<DummyBaseEvent> eventStore = new PostgresEventStore(
            DummyBaseEvent.EVENT_ID_EXTRACTOR,
            DummyBaseEvent.AGGREGATE_ID_EXTRACTOR,
            eventTypeMapper,
            eventBodyMapper,
            dataSource
    )

    @Subject
    def repository = new AggregateRepository<DummyAggregate, DummyBaseEvent>(
            eventStore,
            DummyAggregate.INITIAL_STATE_SUPPLIER,
            DummyAggregate.EVENT_APPLIER,
            AGGREGATE_NAME

    )

    def "find should return aggregate with events applied"() {

        given: 'Events types mapping registered'
            eventTypeMapper.registerMapping(DummyEvent, "DummyEvent", 1)
            eventTypeMapper.registerMapping(AnotherDummyEvent, "AnotherDummyEvent", 1)

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

    }

    static AGGREGATE_NAME = randomAlphanumeric(5)

}
