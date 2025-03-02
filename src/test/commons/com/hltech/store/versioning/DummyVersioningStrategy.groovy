package com.hltech.store.versioning

import com.hltech.store.DummyBaseEvent
import com.hltech.store.DummyEvent
import groovy.json.JsonSlurper

class DummyVersioningStrategy implements EventVersioningStrategy<DummyBaseEvent> {

    static SLURPER = new JsonSlurper()

    @Override
    String toName(Class<? extends DummyBaseEvent> eventType) {
        "DummyEvent"
    }

    @Override
    int toVersion(Class<? extends DummyBaseEvent> eventType) {
        1
    }

    @Override
    String toJson(DummyBaseEvent event) {
        if (event['optionalAttribute'] != null) {
            return """{ "id": "$event.id", "aggregateId": "$event.aggregateId", "optionalAttribute": "$event['optionalAttribute']" }"""
        }
        return """{ "id": "$event.id", "aggregateId": "$event.aggregateId"}"""
    }

    @Override
    DummyBaseEvent toEvent(String eventString, String eventName, int eventVersion) {
        def parsedJson = SLURPER.parseText(eventString)
        def optionalAttribute = parsedJson['optionalAttribute'].toString()
        new DummyEvent(
                UUID.fromString(parsedJson['id'].toString()),
                UUID.fromString(parsedJson['aggregateId'].toString()),
                optionalAttribute == "null" ? null : optionalAttribute
        )
    }

}
