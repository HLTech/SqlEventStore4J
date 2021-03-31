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
        events.eachWithIndex { DummyBaseEvent event, int idx ->
            String payload = eventBodyMapper.eventToString(event)
            def blobedPayload = payload.getBytes(StandardCharsets.UTF_8)
            dbClient.execute(
                    "INSERT INTO EVENT (ID, AGGREGATE_ID, AGGREGATE_NAME, AGGREGATE_VERSION, STREAM_ID, PAYLOAD, EVENT_NAME, EVENT_VERSION) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    [event.id.toString(), event.aggregateId.toString(), aggregateName, idx, STREAM_ID.toString(), blobedPayload, "DummyEvent", 1]
            )
        }
    }

    int getAggregateVersion(
            UUID aggregateId,
            String aggregateName
    ) {
        def aggregateIdString = aggregateId.toString()
        (int) dbClient.firstRow("select max(aggregate_version) as aggregate_version from event where aggregate_id = $aggregateIdString and aggregate_name = $aggregateName")['aggregate_version']
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
