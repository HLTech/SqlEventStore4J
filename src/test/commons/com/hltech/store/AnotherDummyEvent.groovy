package com.hltech.store

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode
@ToString
class AnotherDummyEvent implements DummyBaseEvent {

    UUID id
    UUID aggregateId

    AnotherDummyEvent(
            UUID id,
            UUID aggregateId
    ) {
        this.id = id
        this.aggregateId = aggregateId
    }

    AnotherDummyEvent(UUID aggregateId) {
        this(UUID.randomUUID(), aggregateId)
    }

    AnotherDummyEvent() {
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
