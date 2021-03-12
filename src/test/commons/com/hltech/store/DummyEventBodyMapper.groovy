package com.hltech.store

import groovy.json.JsonSlurper

class DummyEventBodyMapper implements EventBodyMapper<DummyBaseEvent> {

    static SLURPER = new JsonSlurper()

    @Override
    DummyBaseEvent stringToEvent(String eventString, Class<? extends DummyBaseEvent> eventType) {
        def parsedJson = SLURPER.parseText(eventString)
        new DummyEvent(
                UUID.fromString(parsedJson['id'].toString()),
                UUID.fromString(parsedJson['aggregateId'].toString())
        )
    }

    @Override
    String eventToString(DummyBaseEvent event) {
        """{ "id": "$event.id", "aggregateId": "$event.aggregateId" }"""
    }

}
