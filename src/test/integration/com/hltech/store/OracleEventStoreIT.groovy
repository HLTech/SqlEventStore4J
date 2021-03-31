package com.hltech.store

import spock.lang.Subject

import java.nio.charset.StandardCharsets
import java.sql.Blob

class OracleEventStoreIT  extends EventStoreIT implements OracleContainerTest {

    @Subject
    EventStore<DummyBaseEvent> eventStore = new OracleEventStore(
            DummyBaseEvent.EVENT_ID_EXTRACTOR,
            DummyBaseEvent.AGGREGATE_ID_EXTRACTOR,
            eventTypeMapper,
            eventBodyMapper,
            dataSource
    )

    UUID databaseUUIDToUUID(Object databaseUUID) {
        return UUID.fromString((String) databaseUUID)
    }

    String databasePayloadToString(Object databasePayload) {
        getTextFromBlob((Blob) databasePayload)
    }

    void createAggregateInStream(
            UUID aggregateId,
            String aggregateName,
            UUID streamId
    ){
        dbClient.execute("INSERT INTO AGGREGATE_IN_STREAM VALUES (?, ?, ?)", [aggregateId.toString(), aggregateName, streamId.toString()])
    }

    void insertEventsToDatabase(
            List<DummyBaseEvent> events,
            String aggregateName
    ) {
        events.each { DummyBaseEvent event ->
            String payload = eventBodyMapper.eventToString(event)
            def blobedPayload = payload.getBytes(StandardCharsets.UTF_8)
            dbClient.execute(
                    "INSERT INTO EVENT (ID, AGGREGATE_ID, AGGREGATE_NAME, STREAM_ID, PAYLOAD, EVENT_NAME, EVENT_VERSION) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    [event.id.toString(), event.aggregateId.toString(), aggregateName, STREAM_ID.toString(), blobedPayload, "DummyEvent", 1]
            )
        }
    }

    def cleanup() {
        dbClient.execute("delete from event")
        dbClient.execute("delete from aggregate_in_stream")
    }
}
