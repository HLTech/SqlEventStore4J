package com.hltech.store.experimental

import com.hltech.store.DummyBaseEvent
import com.hltech.store.EventStore
import spock.lang.Subject

class ExperimentalPostgresEventStoreIT extends ExperimantalEventStoreIT implements ExperimentalPostgreSQLContainerTest {

    @Subject
    EventStore<DummyBaseEvent> eventStore = new ExperimentalPostgresEventStore(
            DummyBaseEvent.EVENT_ID_EXTRACTOR,
            DummyBaseEvent.AGGREGATE_ID_EXTRACTOR,
            eventTypeMapper,
            eventBodyMapper,
            dataSource
    )

    UUID databaseUUIDToUUID(Object databaseUUID) {
        return (UUID) databaseUUID
    }

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
        events.eachWithIndex { DummyBaseEvent event, int idx ->
            String payload = eventBodyMapper.eventToString(event)
            dbClient.execute(
                    "INSERT INTO EVENT (ID, AGGREGATE_ID, AGGREGATE_NAME, AGGREGATE_VERSION, STREAM_ID, PAYLOAD, EVENT_NAME, EVENT_VERSION) VALUES (?::UUID, ?::UUID, ?, ?, ?::UUID, ?::JSONB, ?, ?)",
                    [event.id, event.aggregateId, aggregateName, idx, ExperimantalEventStoreIT.STREAM_ID, payload, "DummyEvent", 1]
            )
        }
    }

    int getAggregateVersion(
            UUID aggregateId,
            String aggregateName
    ) {
        (int) dbClient.firstRow("select max(aggregate_version) as aggregate_version from event where aggregate_id = $aggregateId and aggregate_name = $aggregateName")['aggregate_version']
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
