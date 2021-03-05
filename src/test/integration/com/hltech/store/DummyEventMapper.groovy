package com.hltech.store

import groovy.json.JsonSlurper

class DummyEventMapper implements EventMapper {

    static SLURPER = new JsonSlurper()

    @Override
    <T extends Event> DummyEvent stringToEvent(String eventString, Class<T> eventType) {
        def parsedJson = SLURPER.parseText(eventString)
        new DummyEvent(
                UUID.fromString(parsedJson['id'].toString()),
                UUID.fromString(parsedJson['aggregateId'].toString())
        )
    }

    @Override
    String eventToString(Event event) {
        """{ "id": "$event.id", "aggregateId": "$event.aggregateId" }"""
    }

}
