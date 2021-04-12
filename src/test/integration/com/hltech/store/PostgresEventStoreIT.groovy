package com.hltech.store


import spock.lang.Subject

class PostgresEventStoreIT extends EventStoreIT implements PostgreSQLContainerTest {

    @Subject
    EventStore<DummyBaseEvent> eventStore = new PostgresEventStore(
            DummyBaseEvent.EVENT_ID_EXTRACTOR,
            DummyBaseEvent.AGGREGATE_ID_EXTRACTOR,
            eventVersioningStrategy,
            eventBodyMapper,
            dataSource
    )

    UUID databaseUUIDToUUID(Object databaseUUID) {
        return (UUID) databaseUUID
    }

    String databasePayloadToString(Object databasePayload) {
        databasePayload.toString()
    }

    UUID createStream(
            UUID aggregateId,
            String aggregateName
    ){
        UUID streamId = UUID.randomUUID()
        dbClient.execute("INSERT INTO AGGREGATE_IN_STREAM VALUES (?::UUID, ?, 0, ?::UUID)", [aggregateId, aggregateName, streamId])
        return streamId
    }

    void insertEventsToDatabase(
            List<DummyBaseEvent> events,
            String aggregateName
    ) {
        events.eachWithIndex { DummyBaseEvent event, int idx ->
            String payload = eventBodyMapper.eventToString(event)
            dbClient.execute(
                    "INSERT INTO EVENT (ID, AGGREGATE_VERSION, STREAM_ID, PAYLOAD, EVENT_NAME, EVENT_VERSION) SELECT ?, ?, stream_id, ?::JSONB, ?, ? from aggregate_in_stream where aggregate_id = ? AND aggregate_name = ?",
                    [event.id, idx, payload, "DummyEvent", 1, event.aggregateId, aggregateName]
            )
            dbClient.execute(
                    "UPDATE aggregate_in_stream SET aggregate_version = aggregate_version + 1 where aggregate_id = ? AND aggregate_name = ?",
                    [event.aggregateId, aggregateName]
            )
        }
    }

    int getAggregateVersion(
            UUID aggregateId,
            String aggregateName
    ) {
        (int) dbClient.firstRow("select aggregate_version from aggregate_in_stream where aggregate_id = $aggregateId and aggregate_name = $aggregateName")['aggregate_version']
    }

    boolean streamExist(
            UUID aggregateId,
            String aggregateName
    ) {
        ((int) dbClient.firstRow("select count(1) from aggregate_in_stream where aggregate_id = $aggregateId and aggregate_name = $aggregateName")[0]) == 1
    }

    def cleanup() {
        dbClient.execute("delete from event")
        dbClient.execute("delete from aggregate_in_stream")
    }

}
