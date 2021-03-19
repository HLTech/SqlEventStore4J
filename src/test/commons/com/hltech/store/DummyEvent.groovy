package com.hltech.store

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode
@ToString
class DummyEvent implements DummyBaseEvent {

    UUID id
    UUID aggregateId

    DummyEvent(
            UUID id,
            UUID aggregateId
    ) {
        this.id = id
        this.aggregateId = aggregateId
    }

    DummyEvent(UUID aggregateId) {
        this(UUID.randomUUID(), aggregateId)
    }

    DummyEvent() {
        this(UUID.randomUUID(), UUID.randomUUID())
    }

    @Override
    UUID getId() {
        id
    }

    @Override
    UUID getAggregateId() {
        aggregateId
    }

}
