package com.hltech.store

import spock.lang.Subject

import static org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils.randomAlphanumeric

class PostgresEventStoreIT extends PostgreSQLContainerTest {

    def eventTypeMapper = new DummyEventTypeMapper()
    def eventBodyMapper = new DummyEventBodyMapper()

    @Subject
    def postgresEventStore = new PostgresEventStore(
            DummyBaseEvent.EVENT_ID_EXTRACTOR,
            DummyBaseEvent.AGGREGATE_ID_EXTRACTOR,
            eventTypeMapper,
            eventBodyMapper,
            dataSource
    )

    def "save should be able to save events in database"() {

        when: 'Events saved'
            AGGREGATE_EVENTS.each { postgresEventStore.save(it, STREAM_NAME) }

        then: 'All events exist in database'
            def rows = dbClient.rows("select * from event where aggregate_id = '${AGGREGATE_ID}' order by order_of_occurrence asc".toString())

        and: 'Table rows for events as expected'
            AGGREGATE_EVENTS.eachWithIndex { DummyBaseEvent event, int idx ->
                assert rows[idx]['id'] == event.id
                assert rows[idx]['aggregate_id'] == event.aggregateId
                assert rows[idx]['payload'].toString().replaceAll(" ", "") == eventBodyMapper.eventToString(event).replaceAll(" ", "")
                assert rows[idx]['order_of_occurrence'] != null
                assert rows[idx]['stream_name'] == STREAM_NAME
                assert rows[idx]['event_name'] == "DummyEvent"
                assert rows[idx]['event_version'] == 1
            }

    }

    def "findAll by aggregateId should return all events related to aggregate in correct order across all streams"() {

        given: 'Events exist in database in two different streams'
            insertEventsToDatabase([AGGREGATE_EVENTS[0]], STREAM_NAME)
            insertEventsToDatabase([AGGREGATE_EVENTS[1]], ANOTHER_STREAM_NAME)

        when: 'Search for events by aggregateId'
            def events = postgresEventStore.findAll(AGGREGATE_ID)

        then: 'Events found'
            events.size() == AGGREGATE_EVENTS.size()

        and: 'Events in correct order'
            AGGREGATE_EVENTS.eachWithIndex { DummyBaseEvent event, int idx ->
                assert event == events[idx]
            }

    }

    def "findAll by aggregateId should not return events when events for given aggregate not exist"() {

        given: 'Events exist in database in two different streams'
            insertEventsToDatabase([AGGREGATE_EVENTS[0]], STREAM_NAME)
            insertEventsToDatabase([AGGREGATE_EVENTS[1]], ANOTHER_STREAM_NAME)

        when: 'Search for events by another aggregateId'
            def events = postgresEventStore.findAll(ANOTHER_AGGREGATE_ID)

        then: 'Events not found'
            events.isEmpty()

    }

    def "findAll by stream name should return all events in the stream in correct order"() {

        given: 'Events for two different aggregates exist in database in single streams'
            def ALL_EVENTS = AGGREGATE_EVENTS + ANOTHER_AGGREGATE_EVENTS
            insertEventsToDatabase(ALL_EVENTS, STREAM_NAME)

        when: 'Search for events by stream name'
            def events = postgresEventStore.findAll(STREAM_NAME)

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

        and: 'Zero event found in another stream name'
            postgresEventStore.findAll(ANOTHER_STREAM_NAME).isEmpty()

    }

    def "findAll by stream name should not return events when events are in a different stream name"() {

        given: 'Events for two different aggregates exist in database in single streams'
            def ALL_EVENTS = AGGREGATE_EVENTS + ANOTHER_AGGREGATE_EVENTS
            insertEventsToDatabase(ALL_EVENTS, STREAM_NAME)

        when: 'Search for events by another stream name'
            def events = postgresEventStore.findAll(ANOTHER_STREAM_NAME)

        then: 'Events not found'
            events.isEmpty()

    }

    def "findAll by aggregateId and streamName should return events related to aggregate and stream in correct order"() {

        given: 'Events exist in database'
            insertEventsToDatabase(AGGREGATE_EVENTS, STREAM_NAME)

        when: 'Search for events by aggregateId and stream name'
            def events = postgresEventStore.findAll(AGGREGATE_ID, STREAM_NAME)

        then: 'Events found'
            events.size() == AGGREGATE_EVENTS.size()

        and: 'Events in correct order'
            AGGREGATE_EVENTS.eachWithIndex { DummyBaseEvent event, int idx ->
                assert event == events[idx]
            }

    }

    def "findAll by aggregateId and streamName should return only events related to aggregate and stream"() {

        given: 'Events exist in database for aggregate and stream name'
            insertEventsToDatabase([AGGREGATE_EVENTS[0]], STREAM_NAME)

        and: 'Events exist in database for aggregate and another stream name'
            insertEventsToDatabase([AGGREGATE_EVENTS[1]], ANOTHER_STREAM_NAME)

        and: 'Events exist in database for another aggregate and stream name'
            insertEventsToDatabase(ANOTHER_AGGREGATE_EVENTS, STREAM_NAME)

        when: 'Search for events by aggregateId and stream name'
            def events = postgresEventStore.findAll(AGGREGATE_ID, STREAM_NAME)

        then: 'Only events for aggregate and stream name returned'
            events == [AGGREGATE_EVENTS[0]]

    }

    def "findAllToEvent should return given event and all other events that occurred before"() {

        given: 'Events exist in database'
            insertEventsToDatabase(AGGREGATE_EVENTS, STREAM_NAME)

        expect: 'Only events occurred at or before given event has been returned'
            AGGREGATE_EVENTS.eachWithIndex { DummyBaseEvent event, int idx ->
                List<DummyEvent> events = postgresEventStore.findAllToEvent(
                        event,
                        STREAM_NAME
                )
                assert events.size() == idx + 1
            }

    }

    def "findAllToEvent should return only those events which belong to given stream name"() {

        given: 'Events exist in database in two different streams'
            insertEventsToDatabase([AGGREGATE_EVENTS[0]], STREAM_NAME)
            insertEventsToDatabase([AGGREGATE_EVENTS[1]], ANOTHER_STREAM_NAME)

        when: 'Search for events in stream'
            List<DummyEvent> events = postgresEventStore.findAllToEvent(
                    AGGREGATE_EVENTS[0],
                    STREAM_NAME
            )

        then: 'Events found'
            !events.empty

        and: 'Only events from stream returned'
            events.id == [AGGREGATE_EVENTS[0].id]

        when: 'Search for events in another stream'
            List<DummyEvent> eventsInAnotherStream = postgresEventStore.findAllToEvent(
                    AGGREGATE_EVENTS[1],
                    ANOTHER_STREAM_NAME
            )

        then: 'Events found'
            !eventsInAnotherStream.empty

        and: 'Only events from another stream returned'
            eventsInAnotherStream.id == [AGGREGATE_EVENTS[1].id]

    }

    def "findAllToEvent should not return events when those are in a different stream name"() {

        given: 'Events exist in database'
            insertEventsToDatabase(AGGREGATE_EVENTS, STREAM_NAME)

        when: 'Search for events in another stream name'
            List<DummyEvent> events = postgresEventStore.findAllToEvent(
                    AGGREGATE_EVENTS.last(),
                    ANOTHER_STREAM_NAME
            )

        then: 'Events not found'
            events.empty

    }

    private void insertEventsToDatabase(
            List<DummyBaseEvent> events,
            String streamName
    ) {
        events.each { DummyBaseEvent event ->
            String payload = eventBodyMapper.eventToString(event)
            dbClient.execute(
                    "INSERT INTO EVENT (ID, AGGREGATE_ID, PAYLOAD, STREAM_NAME, EVENT_NAME, EVENT_VERSION) VALUES (?::UUID, ?::UUID, ?::JSONB, ?, ?, ?)",
                    [event.id, event.aggregateId, payload, streamName, "DummyEvent", 1]
            )
        }
    }

    def cleanup() {
        dbClient.execute("delete from event")
    }

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
    static STREAM_NAME = randomAlphanumeric(5)
    static ANOTHER_STREAM_NAME = randomAlphanumeric(5)

}
