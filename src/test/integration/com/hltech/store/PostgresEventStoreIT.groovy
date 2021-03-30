package com.hltech.store


import spock.lang.Subject

class PostgresEventStoreIT extends EventStoreIT implements PostgreSQLContainerTest {

    @Subject
    EventStore<DummyBaseEvent> eventStore = new PostgresEventStore(
            DummyBaseEvent.EVENT_ID_EXTRACTOR,
            DummyBaseEvent.AGGREGATE_ID_EXTRACTOR,
            eventTypeMapper,
            eventBodyMapper,
            dataSource
    )

    String databasePayloadToString(Object databasePayload) {
        databasePayload.toString()
    }

    void createAggregateInStream(
            UUID aggregateId,
            String aggregateName,
            UUID streamId
    ){
        dbClient.execute("INSERT INTO AGGREGATE_IN_STREAM VALUES (?::UUID, ?, ?::UUID)", [aggregateId, aggregateName, streamId])
    }

    void insertEventsToDatabase(
            List<DummyBaseEvent> events,
            String aggregateName
    ) {
        events.each { DummyBaseEvent event ->
            String payload = eventBodyMapper.eventToString(event)
            dbClient.execute(
                    "INSERT INTO EVENT (ID, AGGREGATE_ID, AGGREGATE_NAME, STREAM_ID, PAYLOAD, EVENT_NAME, EVENT_VERSION) VALUES (?::UUID, ?::UUID, ?, ?::UUID, ?::JSONB, ?, ?)",
                    [event.id, event.aggregateId, aggregateName, STREAM_ID, payload, "DummyEvent", 1]
            )
        }
    }

    def cleanup() {
        dbClient.execute("delete from event")
        dbClient.execute("delete from aggregate_in_stream")
    }

}
