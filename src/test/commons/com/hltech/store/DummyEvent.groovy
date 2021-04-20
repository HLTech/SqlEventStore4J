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
    final String optionalAttribute

    @JsonCreator
    DummyEvent(
            @JsonProperty("id") UUID id,
            @JsonProperty("aggregateId") UUID aggregateId,
            @JsonProperty("optionalAttribute") String optionalAttribute
    ) {
        this.id = id
        this.aggregateId = aggregateId
        this.optionalAttribute = optionalAttribute
    }

    DummyEvent(UUID id, UUID aggregateId) {
        this(id, aggregateId, null)
    }

    DummyEvent(UUID aggregateId) {
        this(UUID.randomUUID(), aggregateId, null)
    }

    DummyEvent() {
        this(UUID.randomUUID(), UUID.randomUUID(), null)
    }

    @Override
    UUID getId() {
        id
    }

    @Override
    UUID getAggregateId() {
        aggregateId
    }

    DummyEvent withOptionalAttribute(String optionalAttribute) {
        new DummyEvent(id, aggregateId, optionalAttribute)
    }

}
