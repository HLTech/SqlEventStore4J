package com.hltech.store

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode
@ToString
class DummyEvent implements DummyBaseEvent {

    final UUID id
    final UUID aggregateId

    @JsonCreator
    DummyEvent(
            @JsonProperty("id") UUID id,
            @JsonProperty("aggregateId") UUID aggregateId
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
