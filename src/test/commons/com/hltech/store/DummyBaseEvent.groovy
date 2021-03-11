package com.hltech.store

import java.util.function.Function

interface DummyBaseEvent {

    static Function<DummyBaseEvent, UUID> EVENT_ID_EXTRACTOR = { it.id }
    static Function<DummyBaseEvent, UUID> AGGREGATE_ID_EXTRACTOR = { it.aggregateId }

    UUID getId()
    UUID getAggregateId()

}
