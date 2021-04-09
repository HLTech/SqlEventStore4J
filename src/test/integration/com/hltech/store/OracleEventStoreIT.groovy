package com.hltech.store

import spock.lang.Subject

import java.nio.charset.StandardCharsets
import java.sql.Blob

class OracleEventStoreIT extends EventStoreIT implements OracleContainerTest {

    @Subject
    EventStore<DummyBaseEvent> eventStore = new OracleEventStore(
            DummyBaseEvent.EVENT_ID_EXTRACTOR,
            DummyBaseEvent.AGGREGATE_ID_EXTRACTOR,
            eventVersionPolicy,
            eventBodyMapper,
            dataSource
    )

    UUID databaseUUIDToUUID(Object databaseUUID) {
        return UUID.fromString((String) databaseUUID)
    }

    String databasePayloadToString(Object databasePayload) {
        getTextFromBlob((Blob) databasePayload)
    }

    UUID createStream(
            UUID aggregateId,
            String aggregateName
    ) {
        UUID streamId = UUID.randomUUID()
        dbClient.execute("INSERT INTO AGGREGATE_IN_STREAM VALUES (?, ?, 0, ?)", [aggregateId.toString(), aggregateName, streamId.toString()])
        return streamId
    }

    void insertEventsToDatabase(
            List<DummyBaseEvent> events,
            String aggregateName
    ) {
        events.eachWithIndex { DummyBaseEvent event, int idx ->
            String payload = eventBodyMapper.eventToString(event)
            def blobedPayload = payload.getBytes(StandardCharsets.UTF_8)
            dbClient.execute(
                    "INSERT INTO EVENT (ID, AGGREGATE_VERSION, STREAM_ID, PAYLOAD, EVENT_NAME, EVENT_VERSION) SELECT ?, ?, stream_id, ?, ?, ? from aggregate_in_stream where aggregate_id = ? AND aggregate_name = ?",
                    [event.id.toString(), idx, blobedPayload, "DummyEvent", 1, event.aggregateId.toString(), aggregateName]
            )
            dbClient.execute(
                    "UPDATE aggregate_in_stream SET aggregate_version = aggregate_version + 1 where aggregate_id = ? AND aggregate_name = ?",
                    [event.aggregateId.toString(), aggregateName]
            )
        }
    }

    int getAggregateVersion(
            UUID aggregateId,
            String aggregateName
    ) {
        def aggregateIdString = aggregateId.toString()
        (int) dbClient.firstRow("select aggregate_version from aggregate_in_stream where aggregate_id = $aggregateIdString and aggregate_name = $aggregateName")['aggregate_version']
    }

    boolean streamExist(
            UUID aggregateId,
            String aggregateName
    ) {
        def aggregateIdString = aggregateId.toString()
        ((int) dbClient.firstRow("select count(1) from aggregate_in_stream where aggregate_id = $aggregateIdString and aggregate_name = $aggregateName")[0]) == 1
    }

    def cleanup() {
        dbClient.execute("delete from event")
        dbClient.execute("delete from aggregate_in_stream")
    }
}
