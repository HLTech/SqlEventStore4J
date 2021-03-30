package com.hltech.store

import spock.lang.Specification

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric

abstract class EventStoreIT extends Specification {

    DummyEventTypeMapper eventTypeMapper = new DummyEventTypeMapper()
    DummyEventBodyMapper eventBodyMapper = new DummyEventBodyMapper()

    def "save should be able to save events in database"() {

        when: 'Events saved'
            AGGREGATE_EVENTS.each { eventStore.save(it, AGGREGATE_NAME) }

        then: 'All events exist in database'
            def rows = dbClient.rows("select * from event where aggregate_id = '${AGGREGATE_ID}' order by order_of_occurrence asc")

        and: 'Table rows for events as expected'
            AGGREGATE_EVENTS.eachWithIndex { DummyBaseEvent event, int idx ->
                assert databaseUUIDToUUID(rows[idx]['id']) == event.id
                assert databaseUUIDToUUID(rows[idx]['aggregate_id']) == event.aggregateId
                assert rows[idx]['aggregate_name'] == AGGREGATE_NAME
                assert rows[idx]['stream_id'] != null
                assert databasePayloadToString(rows[idx]['payload']).replaceAll(" ", "") == eventBodyMapper.eventToString(event).replaceAll(" ", "")
                assert rows[idx]['order_of_occurrence'] != null
                assert rows[idx]['event_name'] == "DummyEvent"
                assert rows[idx]['event_version'] == 1
            }

    }

    def "findAll by aggregate name should return all events for aggregate name in the stream in correct order"() {

        given: 'Stream for aggregates exist'
            createAggregateInStream(AGGREGATE_ID, AGGREGATE_NAME, STREAM_ID)
            createAggregateInStream(ANOTHER_AGGREGATE_ID, AGGREGATE_NAME, STREAM_ID)

        and: 'Events for two different aggregates exist in database in single streams'
            def ALL_EVENTS = AGGREGATE_EVENTS + ANOTHER_AGGREGATE_EVENTS
            insertEventsToDatabase(ALL_EVENTS, AGGREGATE_NAME)

        when: 'Search for events by aggregate name'
            def events = eventStore.findAll(AGGREGATE_NAME)

        then: 'Events found for aggregate'
            events[AGGREGATE_ID].size() == AGGREGATE_EVENTS.size()

        and: 'Events for aggregate in correct order'
            AGGREGATE_EVENTS.eachWithIndex { DummyBaseEvent event, int idx ->
                assert event == events[AGGREGATE_ID][idx]
            }

        and: 'Events found for another aggregate'
            events[ANOTHER_AGGREGATE_ID].size() == ANOTHER_AGGREGATE_EVENTS.size()

        and: 'Events for another aggregate in correct order'
            ANOTHER_AGGREGATE_EVENTS.eachWithIndex { DummyBaseEvent event, int idx ->
                assert event == events[ANOTHER_AGGREGATE_ID][idx]
            }

        and: 'Zero event found for another aggregate name'
            eventStore.findAll(ANOTHER_AGGREGATE_NAME).isEmpty()

    }

    def "findAll by aggregate name should not return events when events has different aggregate name"() {

        given: 'Stream for aggregates exist'
            createAggregateInStream(AGGREGATE_ID, AGGREGATE_NAME, STREAM_ID)
            createAggregateInStream(ANOTHER_AGGREGATE_ID, AGGREGATE_NAME, STREAM_ID)

        and: 'Events for two different aggregates exist in database in single streams'
            def ALL_EVENTS = AGGREGATE_EVENTS + ANOTHER_AGGREGATE_EVENTS
            insertEventsToDatabase(ALL_EVENTS, AGGREGATE_NAME)

        when: 'Search for events by another aggregate name'
            def events = eventStore.findAll(ANOTHER_AGGREGATE_NAME)

        then: 'Events not found'
            events.isEmpty()

    }

    def "findAll by aggregateId and aggregateName should return events related to aggregate in correct order"() {

        given: 'Stream for aggregate exist'
            createAggregateInStream(AGGREGATE_ID, AGGREGATE_NAME, STREAM_ID)

        and: 'Events exist in database'
            insertEventsToDatabase(AGGREGATE_EVENTS, AGGREGATE_NAME)

        when: 'Search for events by aggregateId and aggregate name'
            def events = eventStore.findAll(AGGREGATE_ID, AGGREGATE_NAME)

        then: 'Events found'
            events.size() == AGGREGATE_EVENTS.size()

        and: 'Events in correct order'
            AGGREGATE_EVENTS.eachWithIndex { DummyBaseEvent event, int idx ->
                assert event == events[idx]
            }

    }

    def "findAll by aggregateId and aggregateName should return only events related to aggregate and stream"() {

        given: 'Streams for aggregates exist'
            createAggregateInStream(AGGREGATE_ID, AGGREGATE_NAME, STREAM_ID)
            createAggregateInStream(AGGREGATE_ID, ANOTHER_AGGREGATE_NAME, STREAM_ID)
            createAggregateInStream(ANOTHER_AGGREGATE_ID, AGGREGATE_NAME, STREAM_ID)

        and: 'Events exist in database for aggregate id and aggregate name'
            insertEventsToDatabase([AGGREGATE_EVENTS[0]], AGGREGATE_NAME)

        and: 'Events exist in database for aggregate and another aggregate name'
            insertEventsToDatabase([AGGREGATE_EVENTS[1]], ANOTHER_AGGREGATE_NAME)

        and: 'Events exist in database for another aggregate and aggregate name'
            insertEventsToDatabase(ANOTHER_AGGREGATE_EVENTS, AGGREGATE_NAME)

        when: 'Search for events by aggregateId and aggregate name'
            def events = eventStore.findAll(AGGREGATE_ID, AGGREGATE_NAME)

        then: 'Only events for aggregate name returned'
            events == [AGGREGATE_EVENTS[0]]

    }

    def "findAllToEvent should return given event and all other events that occurred before"() {

        given: 'Stream for aggregate exist'
            createAggregateInStream(AGGREGATE_ID, AGGREGATE_NAME, STREAM_ID)

        and: 'Events exist in database'
            insertEventsToDatabase(AGGREGATE_EVENTS, AGGREGATE_NAME)

        expect: 'Only events occurred at or before given event has been returned'
            AGGREGATE_EVENTS.eachWithIndex { DummyBaseEvent event, int idx ->
                List<DummyBaseEvent> events = eventStore.findAllToEvent(
                        event,
                        AGGREGATE_NAME
                )
                assert events.size() == idx + 1
            }

    }

    def "findAllToEvent should return only those events which belong to given aggregate name"() {

        given: 'Streams for aggregates exist'
            createAggregateInStream(AGGREGATE_ID, AGGREGATE_NAME, STREAM_ID)
            createAggregateInStream(AGGREGATE_ID, ANOTHER_AGGREGATE_NAME, STREAM_ID)

        and: 'Events exist in database in two different streams'
            insertEventsToDatabase([AGGREGATE_EVENTS[0]], AGGREGATE_NAME)
            insertEventsToDatabase([AGGREGATE_EVENTS[1]], ANOTHER_AGGREGATE_NAME)

        when: 'Search for events in stream'
            List<DummyBaseEvent> events = eventStore.findAllToEvent(
                    AGGREGATE_EVENTS[0],
                    AGGREGATE_NAME
            )

        then: 'Events found'
            !events.empty

        and: 'Only events from stream returned'
            events.id == [AGGREGATE_EVENTS[0].id]

        when: 'Search for events in another stream'
            List<DummyBaseEvent> eventsInAnotherStream = eventStore.findAllToEvent(
                    AGGREGATE_EVENTS[1],
                    ANOTHER_AGGREGATE_NAME
            )

        then: 'Events found'
            !eventsInAnotherStream.empty

        and: 'Only events from another stream returned'
            eventsInAnotherStream.id == [AGGREGATE_EVENTS[1].id]

    }

    def "findAllToEvent should not return events when those belong to different aggregate name"() {

        given: 'Streams for aggregates exist'
            createAggregateInStream(AGGREGATE_ID, AGGREGATE_NAME, STREAM_ID)

        and: 'Events exist in database'
            insertEventsToDatabase(AGGREGATE_EVENTS, AGGREGATE_NAME)

        when: 'Search for events for another aggregate name'
            List<DummyBaseEvent> events = eventStore.findAllToEvent(
                    AGGREGATE_EVENTS.last(),
                    ANOTHER_AGGREGATE_NAME
            )

        then: 'Events not found'
            events.empty

    }

    abstract UUID databaseUUIDToUUID(Object databasePayload)

    abstract String databasePayloadToString(Object databasePayload)

    abstract void createAggregateInStream(
            UUID aggregateId,
            String aggregateName,
            UUID streamId
    )

    abstract void insertEventsToDatabase(
            List<DummyBaseEvent> events,
            String aggregateName
    )

    abstract EventStore<DummyBaseEvent> getEventStore()

    static AGGREGATE_ID = UUID.randomUUID()
    static AGGREGATE_EVENTS = [
            new DummyEvent(AGGREGATE_ID),
            new DummyEvent(AGGREGATE_ID)
    ]
    static ANOTHER_AGGREGATE_ID = UUID.randomUUID()
    static ANOTHER_AGGREGATE_EVENTS = [
            new DummyEvent(ANOTHER_AGGREGATE_ID),
            new DummyEvent(ANOTHER_AGGREGATE_ID)
    ]
    static AGGREGATE_NAME = randomAlphanumeric(5)
    static ANOTHER_AGGREGATE_NAME = randomAlphanumeric(5)
    static STREAM_ID = UUID.randomUUID()

}
