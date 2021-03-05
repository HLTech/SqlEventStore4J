package com.hltech.store


import static org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils.randomAlphanumeric

class PostgresEventStoreIT extends PostgreSQLContainerTest {

    def eventTypeMapper = new DummyEventTypeMapper()
    def eventMapper = new DummyEventMapper()

    def postgresEventStore = new PostgresEventStore(eventTypeMapper, eventMapper, dataSource)

    def "save should be able to save events in database"() {

        when: 'Events saved'
            AGGREGATE_EVENTS.each { postgresEventStore.save(it, STREAM_TYPE) }

        then: 'All events exist in database'
            def rows = dbClient.rows("select * from event where aggregate_id = '${AGGREGATE_ID}' order by order_of_occurrence asc".toString())

        and: 'Table rows for events as expected'
            AGGREGATE_EVENTS.eachWithIndex { Event event, int idx ->
                assert rows[idx]['id'] == event.id
                assert rows[idx]['aggregate_id'] == event.aggregateId
                assert rows[idx]['payload'].toString().replaceAll(" ", "") == eventMapper.eventToString(event).replaceAll(" ", "")
                assert rows[idx]['order_of_occurrence'] != null
                assert rows[idx]['stream_type'] == STREAM_TYPE
                assert rows[idx]['event_name'] == "DummyEvent"
                assert rows[idx]['event_version'] == 1
            }

    }

    def "findAll by aggregateId should return all events related to aggregate in correct order across all streams"() {

        given: 'Events exist in database in two different streams'
            insertEventsToDatabase([AGGREGATE_EVENTS[0]], STREAM_TYPE)
            insertEventsToDatabase([AGGREGATE_EVENTS[1]], ANOTHER_STREAM_TYPE)

        when: 'Search for events by aggregateId'
            def events = postgresEventStore.findAll(AGGREGATE_ID)

        then: 'Events found'
            events.size() == AGGREGATE_EVENTS.size()

        and: 'Events in correct order'
            AGGREGATE_EVENTS.eachWithIndex { Event event, int idx ->
                assert event == events[idx]
            }

    }

    def "findAll by aggregateId should not return events when events for given aggregate not exist"() {

        given: 'Events exist in database in two different streams'
            insertEventsToDatabase([AGGREGATE_EVENTS[0]], STREAM_TYPE)
            insertEventsToDatabase([AGGREGATE_EVENTS[1]], ANOTHER_STREAM_TYPE)

        when: 'Search for events by another aggregateId'
            def events = postgresEventStore.findAll(ANOTHER_AGGREGATE_ID)

        then: 'Events not found'
            events.isEmpty()

    }

    def "findAll by stream type should return all events in the stream in correct order"() {

        given: 'Events for two different aggregates exist in database in single streams'
            def ALL_EVENTS = AGGREGATE_EVENTS + ANOTHER_AGGREGATE_EVENTS
            insertEventsToDatabase(ALL_EVENTS, STREAM_TYPE)

        when: 'Search for events by stream type'
            def events = postgresEventStore.findAll(STREAM_TYPE)

        then: 'Events found'
            events.size() == ALL_EVENTS.size()

        and: 'Events in correct order'
            ALL_EVENTS.eachWithIndex { Event event, int idx ->
                assert event == events[idx]
            }

        and: 'Zero event found in another stream type'
            postgresEventStore.findAll(ANOTHER_STREAM_TYPE).isEmpty()

    }

    def "findAll by stream type should not return events when events are in a different stream type"() {

        given: 'Events for two different aggregates exist in database in single streams'
            def ALL_EVENTS = AGGREGATE_EVENTS + ANOTHER_AGGREGATE_EVENTS
            insertEventsToDatabase(ALL_EVENTS, STREAM_TYPE)

        when: 'Search for events by another stream type'
            def events = postgresEventStore.findAll(ANOTHER_STREAM_TYPE)

        then: 'Events not found'
            events.isEmpty()

    }

    def "findAggregate should return aggregate with all events applied in correct order"() {

        given: 'Events exist in database'
            insertEventsToDatabase(AGGREGATE_EVENTS, STREAM_TYPE)

        when: 'Search for aggregate'
            Optional<DummyAggregate> actualAggregate = postgresEventStore.findAggregate(
                    AGGREGATE_ID,
                    STREAM_TYPE,
                    DummyAggregate.INITIAL_STATE_SUPPLIER,
                    DummyAggregate.EVENT_APPLIER
            )

        then: 'Aggregate found'
            actualAggregate.isPresent()

        then: 'Events applied'
            actualAggregate.get().appliedEvents.size() == AGGREGATE_EVENTS.size()

        and: 'Events applied in correct order'
            AGGREGATE_EVENTS.eachWithIndex { Event event, int idx ->
                assert event == actualAggregate.get().appliedEvents[idx]
            }

    }

    def "findAggregate should return aggregate and apply only those events which belong to given stream type"() {

        given: 'Events for aggregate exist in database in two different streams'
            insertEventsToDatabase([AGGREGATE_EVENTS[0]], STREAM_TYPE)
            insertEventsToDatabase([AGGREGATE_EVENTS[1]], ANOTHER_STREAM_TYPE)

        when: 'Search for aggregate in stream'
            Optional<DummyAggregate> aggregateInStream = postgresEventStore.findAggregate(
                    AGGREGATE_ID,
                    STREAM_TYPE,
                    DummyAggregate.INITIAL_STATE_SUPPLIER,
                    DummyAggregate.EVENT_APPLIER
            )

        then: 'Aggregate found'
            aggregateInStream.isPresent()

        and: 'Only events from stream applied on aggregate'
            aggregateInStream.get().appliedEvents.id == [AGGREGATE_EVENTS[0].id]

        when: 'Search for aggregate in another stream'
            Optional<DummyAggregate> aggregateInAnotherStream = postgresEventStore.findAggregate(
                    AGGREGATE_ID,
                    ANOTHER_STREAM_TYPE,
                    DummyAggregate.INITIAL_STATE_SUPPLIER,
                    DummyAggregate.EVENT_APPLIER
            )

        then: 'Aggregate found'
            aggregateInAnotherStream.isPresent()

        and: 'Only events from another stream applied on aggregate'
            aggregateInAnotherStream.get().appliedEvents.id == [AGGREGATE_EVENTS[1].id]

    }

    def "findAggregate should not return aggregate if one is in a different stream type"() {

        given: 'Events exist in database'
            insertEventsToDatabase(AGGREGATE_EVENTS, STREAM_TYPE)

        when: 'Search aggregate in another stream type'
            Optional<DummyAggregate> actualAggregate = postgresEventStore.findAggregate(
                    AGGREGATE_ID,
                    ANOTHER_STREAM_TYPE,
                    DummyAggregate.INITIAL_STATE_SUPPLIER,
                    DummyAggregate.EVENT_APPLIER
            )

        then: 'Aggregate not found'
            !actualAggregate.isPresent()

    }

    def "getAggregate should return aggregate with all events applied in correct order"() {

        given: 'Events exist in database'
            insertEventsToDatabase(AGGREGATE_EVENTS, STREAM_TYPE)

        when: 'Search for aggregate'
            DummyAggregate actualAggregate = postgresEventStore.getAggregate(
                    AGGREGATE_ID,
                    STREAM_TYPE,
                    DummyAggregate.INITIAL_STATE_SUPPLIER,
                    DummyAggregate.EVENT_APPLIER
            )

        then: 'Aggregate found'
            actualAggregate != null

        then: 'Events applied'
            actualAggregate.appliedEvents.size() == AGGREGATE_EVENTS.size()

        and: 'Events applied in correct order'
            AGGREGATE_EVENTS.eachWithIndex { Event event, int idx ->
                assert event == actualAggregate.appliedEvents[idx]
            }

    }

    def "getAggregate should return aggregate and apply only those events which belong to given stream type"() {

        given: 'Events for aggregate exist in database in two different streams'
            insertEventsToDatabase([AGGREGATE_EVENTS[0]], STREAM_TYPE)
            insertEventsToDatabase([AGGREGATE_EVENTS[1]], ANOTHER_STREAM_TYPE)

        when: 'Search for aggregate in stream'
            DummyAggregate aggregateInStream = postgresEventStore.getAggregate(
                    AGGREGATE_ID,
                    STREAM_TYPE,
                    DummyAggregate.INITIAL_STATE_SUPPLIER,
                    DummyAggregate.EVENT_APPLIER
            )

        then: 'Aggregate found'
            aggregateInStream != null

        and: 'Only events from stream applied on aggregate'
            aggregateInStream.appliedEvents.id == [AGGREGATE_EVENTS[0].id]

        when: 'Search for aggregate in another stream'
            DummyAggregate aggregateInAnotherStream = postgresEventStore.getAggregate(
                    AGGREGATE_ID,
                    ANOTHER_STREAM_TYPE,
                    DummyAggregate.INITIAL_STATE_SUPPLIER,
                    DummyAggregate.EVENT_APPLIER
            )

        then: 'Aggregate found'
            aggregateInAnotherStream != null

        and: 'Only events from another stream applied on aggregate'
            aggregateInAnotherStream.appliedEvents.id == [AGGREGATE_EVENTS[1].id]

    }

    def "getAggregate should throw exception if aggregate is in a different stream type"() {

        given: 'Events exist in database'
            insertEventsToDatabase(AGGREGATE_EVENTS, STREAM_TYPE)

        when: 'Search aggregate in another stream type'
            postgresEventStore.getAggregate(
                    AGGREGATE_ID,
                    ANOTHER_STREAM_TYPE,
                    DummyAggregate.INITIAL_STATE_SUPPLIER,
                    DummyAggregate.EVENT_APPLIER
            )

        then: 'Aggregate not found'
            def ex = thrown(IllegalStateException)
            ex.message == "Could not find aggregate with id: ${AGGREGATE_ID} in stream: ${ANOTHER_STREAM_TYPE}"

    }

    def "findAggregateToEvent should return aggregate with given event and all other events that occurred before applied"() {

        given: 'Events exist in database'
            insertEventsToDatabase(AGGREGATE_EVENTS, STREAM_TYPE)

        expect: 'Only events occurred at or before given event has been applied'
            AGGREGATE_EVENTS.eachWithIndex { Event event, int idx ->
                Optional<DummyAggregate> actualAggregate = postgresEventStore.findAggregateToEvent(
                        event,
                        STREAM_TYPE,
                        DummyAggregate.INITIAL_STATE_SUPPLIER,
                        DummyAggregate.EVENT_APPLIER
                )
                assert actualAggregate.isPresent()
                assert actualAggregate.get().appliedEvents.size() == idx + 1
            }

    }

    def "findAggregateToEvent should return aggregate and apply only those events which belong to given stream type"() {

        given: 'Events for aggregate exist in database in two different streams'
            insertEventsToDatabase([AGGREGATE_EVENTS[0]], STREAM_TYPE)
            insertEventsToDatabase([AGGREGATE_EVENTS[1]], ANOTHER_STREAM_TYPE)

        when: 'Search for aggregate in stream'
            Optional<DummyAggregate> aggregateInStream = postgresEventStore.findAggregateToEvent(
                    AGGREGATE_EVENTS[0],
                    STREAM_TYPE,
                    DummyAggregate.INITIAL_STATE_SUPPLIER,
                    DummyAggregate.EVENT_APPLIER
            )

        then: 'Aggregate found'
            aggregateInStream.isPresent()

        and: 'Only events from stream applied on aggregate'
            aggregateInStream.get().appliedEvents.id == [AGGREGATE_EVENTS[0].id]

        when: 'Search for aggregate in another stream'
            Optional<DummyAggregate> aggregateInAnotherStream = postgresEventStore.findAggregateToEvent(
                    AGGREGATE_EVENTS[1],
                    ANOTHER_STREAM_TYPE,
                    DummyAggregate.INITIAL_STATE_SUPPLIER,
                    DummyAggregate.EVENT_APPLIER
            )

        then: 'Aggregate found'
            aggregateInAnotherStream.isPresent()

        and: 'Only events from another stream applied on aggregate'
            aggregateInAnotherStream.get().appliedEvents.id == [AGGREGATE_EVENTS[1].id]

    }

    def "findAggregateToEvent should not return aggregate if one is in a different stream type"() {

        given: 'Events exist in database'
            insertEventsToDatabase(AGGREGATE_EVENTS, STREAM_TYPE)

        when: 'Search aggregate in another stream type'
            Optional<DummyAggregate> actualAggregate = postgresEventStore.findAggregateToEvent(
                    AGGREGATE_EVENTS.last(),
                    ANOTHER_STREAM_TYPE,
                    DummyAggregate.INITIAL_STATE_SUPPLIER,
                    DummyAggregate.EVENT_APPLIER
            )

        then: 'Aggregate not found'
            !actualAggregate.isPresent()

    }

    def "getAggregateToEvent should return aggregate with given event and all other events that occurred before applied"() {

        given: 'Events exist in database'
            insertEventsToDatabase(AGGREGATE_EVENTS, STREAM_TYPE)

        expect: 'Only events occurred at or before given event has been applied'
            AGGREGATE_EVENTS.eachWithIndex { Event event, int idx ->
                DummyAggregate actualAggregate = postgresEventStore.getAggregateToEvent(
                        event,
                        STREAM_TYPE,
                        DummyAggregate.INITIAL_STATE_SUPPLIER,
                        DummyAggregate.EVENT_APPLIER
                )
                assert actualAggregate
                assert actualAggregate.appliedEvents.size() == idx + 1
            }

    }

    def "getAggregateToEvent should return aggregate and apply only those events which belong to given stream type"() {

        given: 'Events for aggregate exist in database in two different streams'
            insertEventsToDatabase([AGGREGATE_EVENTS[0]], STREAM_TYPE)
            insertEventsToDatabase([AGGREGATE_EVENTS[1]], ANOTHER_STREAM_TYPE)

        when: 'Search for aggregate in stream'
            DummyAggregate aggregateInStream = postgresEventStore.getAggregateToEvent(
                    AGGREGATE_EVENTS[0],
                    STREAM_TYPE,
                    DummyAggregate.INITIAL_STATE_SUPPLIER,
                    DummyAggregate.EVENT_APPLIER
            )

        then: 'Aggregate found'
            aggregateInStream != null

        and: 'Only events from stream applied on aggregate'
            aggregateInStream.appliedEvents.id == [AGGREGATE_EVENTS[0].id]

        when: 'Search for aggregate in another stream'
            DummyAggregate aggregateInAnotherStream = postgresEventStore.getAggregateToEvent(
                    AGGREGATE_EVENTS[1],
                    ANOTHER_STREAM_TYPE,
                    DummyAggregate.INITIAL_STATE_SUPPLIER,
                    DummyAggregate.EVENT_APPLIER
            )

        then: 'Aggregate found'
            aggregateInAnotherStream != null

        and: 'Only events from another stream applied on aggregate'
            aggregateInAnotherStream.appliedEvents.id == [AGGREGATE_EVENTS[1].id]

    }

    def "getAggregateToEvent should throw exception if aggregate is in another stream type"() {

        given: 'Events exist in database'
            insertEventsToDatabase(AGGREGATE_EVENTS, STREAM_TYPE)

        when: 'Search aggregate in another stream type'
            postgresEventStore.getAggregateToEvent(
                    AGGREGATE_EVENTS.last(),
                    ANOTHER_STREAM_TYPE,
                    DummyAggregate.INITIAL_STATE_SUPPLIER,
                    DummyAggregate.EVENT_APPLIER
            )

        then: 'Exception thrown'
            def ex = thrown(IllegalStateException)

        and: 'Message as expected'
            ex.message == "Could not find aggregate to event with id: ${AGGREGATE_EVENTS.last().id} in stream: ${ANOTHER_STREAM_TYPE}"

    }

    def "findAllAggregate should return all aggregates by given stream type"() {

        given: 'Events for two aggregates exist in database'
            insertEventsToDatabase(AGGREGATE_EVENTS, STREAM_TYPE)
            insertEventsToDatabase(ANOTHER_AGGREGATE_EVENTS, STREAM_TYPE)

        when: 'Search for aggregate'
            List<DummyAggregate> aggregates = postgresEventStore.findAllAggregate(
                    STREAM_TYPE,
                    DummyAggregate.INITIAL_STATE_SUPPLIER,
                    DummyAggregate.EVENT_APPLIER
            )

        then: 'Aggregates found'
            aggregates.size() == 2

        and: 'Events applied in correct order for both aggregates'
            AGGREGATE_EVENTS.eachWithIndex { Event event, int idx ->
                assert event == aggregates[0].appliedEvents[idx]
            }
            ANOTHER_AGGREGATE_EVENTS.eachWithIndex { Event event, int idx ->
                assert event == aggregates[1].appliedEvents[idx]
            }
    }

    def "findAllAggregate should return aggregate and apply only those events which belong to given stream type"() {

        given: 'Events for aggregate exist in database in two different streams'
            insertEventsToDatabase([AGGREGATE_EVENTS[0]], STREAM_TYPE)
            insertEventsToDatabase([AGGREGATE_EVENTS[1]], ANOTHER_STREAM_TYPE)

        when: 'Search for aggregate in stream'
            List<DummyAggregate> aggregateInStream = postgresEventStore.findAllAggregate(
                    STREAM_TYPE,
                    DummyAggregate.INITIAL_STATE_SUPPLIER,
                    DummyAggregate.EVENT_APPLIER
            )

        then: 'Aggregate found'
            aggregateInStream.size() == 1

        and: 'Only events from stream applied on aggregate'
            aggregateInStream[0].appliedEvents.id == [AGGREGATE_EVENTS[0].id]

        when: 'Search for aggregate in another stream'
            List<DummyAggregate> aggregateInAnotherStream = postgresEventStore.findAllAggregate(
                    ANOTHER_STREAM_TYPE,
                    DummyAggregate.INITIAL_STATE_SUPPLIER,
                    DummyAggregate.EVENT_APPLIER
            )

        then: 'Aggregate found'
            aggregateInAnotherStream.size() == 1

        and: 'Only events from another stream applied on aggregate'
            aggregateInAnotherStream[0].appliedEvents.id == [AGGREGATE_EVENTS[1].id]

    }

    def "findAllAggregate should not return aggregate if one is in a different stream type"() {

        given: 'Events exist in database'
            insertEventsToDatabase(AGGREGATE_EVENTS, STREAM_TYPE)

        when: 'Search aggregate in another stream type'
            List<DummyAggregate> actualAggregate = postgresEventStore.findAllAggregate(
                    ANOTHER_STREAM_TYPE,
                    DummyAggregate.INITIAL_STATE_SUPPLIER,
                    DummyAggregate.EVENT_APPLIER
            )

        then: 'Aggregate not found'
            actualAggregate.isEmpty()

    }

    private void insertEventsToDatabase(
            List<Event> events,
            String streamType
    ) {
        events.each { Event event ->
            String payload = eventMapper.eventToString(event)
            dbClient.execute(
                    "INSERT INTO EVENT (ID, AGGREGATE_ID, PAYLOAD, STREAM_TYPE, EVENT_NAME, EVENT_VERSION) VALUES (?::UUID, ?::UUID, ?::JSONB, ?, ?, ?)",
                    [event.id, event.aggregateId, payload, streamType, "DummyEvent", 1]
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
    static STREAM_TYPE = randomAlphanumeric(5)
    static ANOTHER_STREAM_TYPE = randomAlphanumeric(5)

}
