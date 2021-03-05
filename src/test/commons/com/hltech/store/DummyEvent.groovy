package com.hltech.store

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class DummyEvent implements Event {

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

    @Override
    UUID getId() {
        id
    }

    @Override
    UUID getAggregateId() {
        aggregateId
    }

}
