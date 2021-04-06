package com.hltech.store

import spock.lang.Specification

import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

import static java.util.concurrent.CompletableFuture.runAsync
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric

abstract class EventStoreIT extends Specification {

    DummyEventTypeMapper eventTypeMapper = new DummyEventTypeMapper()
    DummyEventBodyMapper eventBodyMapper = new DummyEventBodyMapper()

    def "save should be able to save events in database"() {

        given: 'Stream for aggregates exist'
            UUID streamId = createStream(AGGREGATE_ID, AGGREGATE_NAME)

        when: 'Events saved'
            AGGREGATE_EVENTS.each { eventStore.save(it, AGGREGATE_NAME) }

        then: 'All events exist in database'
            def rows = dbClient.rows("select * from event where stream_id = '$streamId' order by aggregate_version asc")

        and: 'Table rows for events as expected'
            AGGREGATE_EVENTS.eachWithIndex { DummyBaseEvent event, int idx ->
                assert databaseUUIDToUUID(rows[idx]['id']) == event.id
                assert rows[idx]['aggregate_version'] == idx + 1
                assert databaseUUIDToUUID(rows[idx]['stream_id']) == streamId
                assert databasePayloadToString(rows[idx]['payload']).replaceAll(" ", "") == eventBodyMapper.eventToString(event).replaceAll(" ", "")
                assert rows[idx]['order_of_occurrence'] != null
                assert rows[idx]['event_name'] == "DummyEvent"
                assert rows[idx]['event_version'] == 1
            }

    }

    def "save in parallel for single aggregate should set valid aggregate version"() {

        given: 'Stream for aggregate exist'
            createStream(AGGREGATE_ID, AGGREGATE_NAME)

        when: 'Saving 100 events in parallel for aggregate'
            def threadPool = Executors.newFixedThreadPool(10)
            (1..100).collect {
                threadPool.submit { eventStore.save(new DummyEvent(AGGREGATE_ID), AGGREGATE_NAME) }
            }.each { it.get() }

        then: 'Actual aggregate version is 100'
            assert getAggregateVersion(AGGREGATE_ID, AGGREGATE_NAME) == 100

        cleanup:
            threadPool.shutdown()

    }

    def "save in parallel for multiple aggregate should set valid aggregates versions"() {

        given: 'Stream for aggregates exist'
            createStream(AGGREGATE_ID, AGGREGATE_NAME)
            createStream(AGGREGATE_ID, ANOTHER_AGGREGATE_NAME)
            createStream(ANOTHER_AGGREGATE_ID, AGGREGATE_NAME)
            createStream(ANOTHER_AGGREGATE_ID, ANOTHER_AGGREGATE_NAME)

        when: 'Saving events in parallel for multiple aggregates'
            def threadPool = Executors.newFixedThreadPool(10)
            [
                    runAsync {
                        (1..70).collect {
                            threadPool.submit { eventStore.save(new DummyEvent(AGGREGATE_ID), AGGREGATE_NAME) }
                        }.each { it.get() }
                    },
                    runAsync {
                        (1..30).collect {
                            threadPool.submit { eventStore.save(new DummyEvent(AGGREGATE_ID), ANOTHER_AGGREGATE_NAME) }
                        }.each { it.get() }
                    },
                    runAsync {
                        (1..50).collect {
                            threadPool.submit { eventStore.save(new DummyEvent(ANOTHER_AGGREGATE_ID), AGGREGATE_NAME) }
                        }.each { it.get() }
                    },
                    runAsync {
                        (1..80).collect {
                            threadPool.submit { eventStore.save(new DummyEvent(ANOTHER_AGGREGATE_ID), ANOTHER_AGGREGATE_NAME) }
                        }.each { it.get() }
                    }
            ].each { it.get() }

        then: 'Actual versions of aggregates as expected'
            assert getAggregateVersion(AGGREGATE_ID, AGGREGATE_NAME) == 70
            assert getAggregateVersion(AGGREGATE_ID, ANOTHER_AGGREGATE_NAME) == 30
            assert getAggregateVersion(ANOTHER_AGGREGATE_ID, AGGREGATE_NAME) == 50
            assert getAggregateVersion(ANOTHER_AGGREGATE_ID, ANOTHER_AGGREGATE_NAME) == 80

        cleanup:
            threadPool.shutdown()

    }

    def "save should create stream for aggregate when it does not exist"() {

        when: 'Save event'
            eventStore.save(AGGREGATE_EVENTS[0], AGGREGATE_NAME)

        then: 'Stream created'
            def row  = dbClient.firstRow("select * from aggregate_in_stream where aggregate_id = '$AGGREGATE_ID' and aggregate_name = $AGGREGATE_NAME")
            row['stream_id'] != null

    }

    def "save with optimistic locking should be able to save events in database"() {

        given: 'Stream for aggregates exist'
            UUID streamId = createStream(AGGREGATE_ID, AGGREGATE_NAME)

        when: 'Events saved'
            AGGREGATE_EVENTS.eachWithIndex { DummyBaseEvent event, int expectedAggregateVersion ->
                eventStore.save(event, AGGREGATE_NAME, expectedAggregateVersion)
            }

        then: 'All events exist in database'
            def rows = dbClient.rows("select * from event where stream_id = '$streamId' order by aggregate_version asc")

        and: 'Table rows for events as expected'
            AGGREGATE_EVENTS.eachWithIndex { DummyBaseEvent event, int idx ->
                assert databaseUUIDToUUID(rows[idx]['id']) == event.id
                assert rows[idx]['aggregate_version'] == idx + 1
                assert databaseUUIDToUUID(rows[idx]['stream_id']) == streamId
                assert databasePayloadToString(rows[idx]['payload']).replaceAll(" ", "") == eventBodyMapper.eventToString(event).replaceAll(" ", "")
                assert rows[idx]['order_of_occurrence'] != null
                assert rows[idx]['event_name'] == "DummyEvent"
                assert rows[idx]['event_version'] == 1
            }

    }

    def "save with optimistic locking in parallel with same expectedAggregateVersion should success only for first attempt "() {

        given: 'Stream for aggregates exist'
            createStream(AGGREGATE_ID, AGGREGATE_NAME)

        and: 'Aggregate version is 2'
            eventStore.save(new DummyEvent(AGGREGATE_ID), AGGREGATE_NAME)
            eventStore.save(new DummyEvent(AGGREGATE_ID), AGGREGATE_NAME)

        and: 'Optimistic lock exception counter value is 0'
            AtomicInteger optimisticLockingExceptionCounter = new AtomicInteger()

        when: 'Saving events with optimistic locking in parallel'
            def threadPool = Executors.newFixedThreadPool(10)
            (0..9).collect {
                threadPool.submit {
                    try {
                        eventStore.save(new DummyEvent(AGGREGATE_ID), AGGREGATE_NAME, 2)
                    } catch (OptimisticLockingException ex) {
                        optimisticLockingExceptionCounter.incrementAndGet()
                    }
                }
            }.each { it.get() }

        then: 'Actual versions of aggregates as expected'
            assert getAggregateVersion(AGGREGATE_ID, AGGREGATE_NAME) == 3

        and: 'Number of attempts that ends with optimistic lock exception as expected'
            optimisticLockingExceptionCounter.get() == 9

        cleanup:
            threadPool.shutdown()

    }

    def "save with optimistic locking in parallel for multiple aggregate should set valid aggregates versions"() {

        given: 'Stream for aggregates exist'
            createStream(AGGREGATE_ID, AGGREGATE_NAME)
            createStream(AGGREGATE_ID, ANOTHER_AGGREGATE_NAME)
            createStream(ANOTHER_AGGREGATE_ID, AGGREGATE_NAME)
            createStream(ANOTHER_AGGREGATE_ID, ANOTHER_AGGREGATE_NAME)

        when: 'Saving events with optimistic locking in parallel for multiple aggregates'
            def threadPool = Executors.newFixedThreadPool(10)
            [
                    runAsync {
                        (0..70).each { expectedAggregateVersion ->
                            eventStore.save(new DummyEvent(AGGREGATE_ID), AGGREGATE_NAME, expectedAggregateVersion)
                        }
                    },
                    runAsync {
                        (0..30).each { expectedAggregateVersion ->
                            eventStore.save(new DummyEvent(AGGREGATE_ID), ANOTHER_AGGREGATE_NAME, expectedAggregateVersion)
                        }
                    },
                    runAsync {
                        (0..50).each { expectedAggregateVersion ->
                            eventStore.save(new DummyEvent(ANOTHER_AGGREGATE_ID), AGGREGATE_NAME, expectedAggregateVersion)
                        }
                    },
                    runAsync {
                        (0..80).each { expectedAggregateVersion ->
                            eventStore.save(new DummyEvent(ANOTHER_AGGREGATE_ID), ANOTHER_AGGREGATE_NAME, expectedAggregateVersion)
                        }
                    }
            ].each { it.get() }

        then: 'Actual versions of aggregates as expected'
            assert getAggregateVersion(AGGREGATE_ID, AGGREGATE_NAME) == 71
            assert getAggregateVersion(AGGREGATE_ID, ANOTHER_AGGREGATE_NAME) == 31
            assert getAggregateVersion(ANOTHER_AGGREGATE_ID, AGGREGATE_NAME) == 51
            assert getAggregateVersion(ANOTHER_AGGREGATE_ID, ANOTHER_AGGREGATE_NAME) == 81

        cleanup:
            threadPool.shutdown()

    }

    def "save with optimistic locking should throw exception when expected version is lower than actual"() {

        given: 'Stream for aggregates exist'
            createStream(AGGREGATE_ID, AGGREGATE_NAME)

        and: 'Aggregate is in version 2'
            eventStore.save(new DummyEvent(AGGREGATE_ID), AGGREGATE_NAME)
            eventStore.save(new DummyEvent(AGGREGATE_ID), AGGREGATE_NAME)

        when: 'Save with expected version 1'
            eventStore.save(new DummyEvent(AGGREGATE_ID), AGGREGATE_NAME, 1)

        then: 'Exception thrown'
            def ex = thrown(OptimisticLockingException)
            ex.message == "Could not save event to database with aggregateId $AGGREGATE_ID, aggregateName $AGGREGATE_NAME and expectedVersion 1"

    }

    def "save with optimistic locking should throw exception when expected version is higher than actual"() {

        given: 'Stream for aggregates exist'
            createStream(AGGREGATE_ID, AGGREGATE_NAME)

        and: 'Aggregate is in version 2'
            eventStore.save(new DummyEvent(AGGREGATE_ID), AGGREGATE_NAME)
            eventStore.save(new DummyEvent(AGGREGATE_ID), AGGREGATE_NAME)

        when: 'Save with expected version 3'
            eventStore.save(new DummyEvent(AGGREGATE_ID), AGGREGATE_NAME, 3)

        then: 'Exception thrown'
            def ex = thrown(OptimisticLockingException)
            ex.message == "Could not save event to database with aggregateId $AGGREGATE_ID, aggregateName $AGGREGATE_NAME and expectedVersion 3"

    }

    def "save with optimistic locking should create stream for aggregate when it does not exist"() {

        when: 'Save event'
            eventStore.save(AGGREGATE_EVENTS[0], AGGREGATE_NAME, 0)

        then: 'Stream created'
            def row  = dbClient.firstRow("select * from aggregate_in_stream where aggregate_id = '$AGGREGATE_ID' and aggregate_name = $AGGREGATE_NAME")
            row['stream_id'] != null

    }

    def "findAll by aggregate name should return all events for aggregate name in the stream in correct order"() {

        given: 'Stream for aggregates exist'
            createStream(AGGREGATE_ID, AGGREGATE_NAME)
            createStream(ANOTHER_AGGREGATE_ID, AGGREGATE_NAME)

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
            createStream(AGGREGATE_ID, AGGREGATE_NAME)
            createStream(ANOTHER_AGGREGATE_ID, AGGREGATE_NAME)

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
            createStream(AGGREGATE_ID, AGGREGATE_NAME)

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
            createStream(AGGREGATE_ID, AGGREGATE_NAME)
            createStream(AGGREGATE_ID, ANOTHER_AGGREGATE_NAME)
            createStream(ANOTHER_AGGREGATE_ID, AGGREGATE_NAME)

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
            createStream(AGGREGATE_ID, AGGREGATE_NAME)

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
            createStream(AGGREGATE_ID, AGGREGATE_NAME)
            createStream(AGGREGATE_ID, ANOTHER_AGGREGATE_NAME)

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
            createStream(AGGREGATE_ID, AGGREGATE_NAME)

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

    abstract UUID createStream(
            UUID aggregateId,
            String aggregateName
    )

    abstract void insertEventsToDatabase(
            List<DummyBaseEvent> events,
            String aggregateName
    )

    abstract EventStore<DummyBaseEvent> getEventStore()

    abstract int getAggregateVersion(
            UUID aggregateId,
            String aggregateName
    )

    abstract boolean streamExist(
            UUID aggregateId,
            String aggregateName
    )

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
