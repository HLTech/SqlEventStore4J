package com.hltech.store

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode
@ToString
class DummyEventV2 implements DummyBaseEvent {

    final UUID id
    final UUID aggregateId
    final String client;

    @JsonCreator
    DummyEventV2(
            @JsonProperty("id") UUID id,
            @JsonProperty("aggregateId") UUID aggregateId,
            @JsonProperty("client") String client
    ) {
        this.id = id
        this.aggregateId = aggregateId
        this.client = client
    }


    DummyEventV2() {
        this(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID().toString())
    }

    DummyEventV2(String client) {
        this(UUID.randomUUID(), UUID.randomUUID(), client)
    }

    @Override
    UUID getId() {
        id
    }

    @Override
    UUID getAggregateId() {
        aggregateId
    }

    String getClient() {
        client
    }

}
