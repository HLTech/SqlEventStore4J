package com.hltech.store.versioning

import com.hltech.store.DummyBaseEvent
import com.hltech.store.DummyEvent
import com.hltech.store.DummyEventV2
import groovy.json.JsonSlurper
import spock.lang.Specification
import spock.lang.Subject

import java.util.function.Function

class WrappingBasedVersioningUT extends Specification {

    static jsonSlurper = new JsonSlurper()
    static eventName = "DummyEvent"
    static dummyEvent = new DummyEvent()
    static defaultClient = "Default John"
    static v3RenamedClientField = "Renamed John"

    static eventJsonV1 = """{"id":"$dummyEvent.id","aggregateId":"$dummyEvent.aggregateId"}"""
    static eventJsonV2 = """{"id":"$dummyEvent.id","aggregateId":"$dummyEvent.aggregateId", "client":"$defaultClient"}"""
    static eventJsonV3 = """{"id":"$dummyEvent.id","aggregateId":"$dummyEvent.aggregateId", "renamedClient":"$v3RenamedClientField"}"""
    static dummyEventV2 = new DummyEventV2(dummyEvent.id, dummyEvent.aggregateId, defaultClient)
    static dummyEventV3 = new DummyEventV2(dummyEvent.id, dummyEvent.aggregateId, v3RenamedClientField)


    static constantVersionNumber = 1
    @Subject
    def eventVersioningStrategy = new WrappingBasedVersioning<DummyBaseEvent>()

    def 'toEvent should return same event when registered wrapper do not change anything while deserializing'() {

        given: 'Wrapper which do some parsing and return expected event'
            Function<String, DummyEvent> eventWrapper = { json -> dummyEvent }

        and: 'EventWrapper registered for eventName and eventType'
            eventVersioningStrategy.registerEvent(DummyEvent.class, "DummyEvent", eventWrapper)

        expect: 'toEvent return expected event'
            dummyEvent == eventVersioningStrategy.toEvent(eventJsonV1, eventName, constantVersionNumber)

    }

    def 'toEvent should be able to parse json with new field from new instance and just ignore new value'() {

        given: 'Wrapper which do some parsing and return expected event'
            Function<String, DummyEvent> eventWrapper = { json -> dummyEvent }

        and: 'EventWrapper registered for eventName and eventType'
            eventVersioningStrategy.registerEvent(DummyEvent.class, "DummyEvent", eventWrapper)

        expect: 'toEvent return expected event'
            dummyEvent == eventVersioningStrategy.toEvent(eventJsonV2, eventName, constantVersionNumber)

    }

    def 'toEvent should use default for field which was added in v2 of event but still old json arrives'() {

        given: 'Wrapper which do some parsing and return expected event'

            Function<String, DummyEvent> eventWrapper = { json ->
                def object = jsonSlurper.parseText(json)
                assert object instanceof Map
                if (object.client == null) new DummyEventV2(UUID.fromString(object.id), UUID.fromString(object.aggregateId), defaultClient)
            }

        and: 'EventWrapper registered for eventName and eventType'
            eventVersioningStrategy.registerEvent(DummyEvent.class, eventName, eventWrapper)

        expect: 'toEvent return event with filled field with default value'
            dummyEventV2 == eventVersioningStrategy.toEvent(eventJsonV1, eventName, constantVersionNumber)

    }

    def 'toEvent should use registered wrapper to solve renamed cases'() {

        given: 'Wrapper which do some parsing and return expected event'

            Function<String, DummyEvent> eventWrapper = { json ->
                def object = jsonSlurper.parseText(json)
                assert object instanceof Map
                if (object.renamedClient != null) new DummyEventV2(UUID.fromString(object.id), UUID.fromString(object.aggregateId), object.renamedClient)
                else if (object.client == null) new DummyEventV2(UUID.fromString(object.id), UUID.fromString(object.aggregateId), defaultClient)
            }

        and: 'EventWrapper registered for eventName and eventType'
            eventVersioningStrategy.registerEvent(DummyEvent.class, eventName, eventWrapper)

        expect: '''toEvent return event with filled field with passed in "renamedClient" field
                    comparing that to dummyEvent in v2 version as someone could prepare wrapper for new json, but underlying event class do not really change'''
            dummyEventV3 == eventVersioningStrategy.toEvent(eventJsonV3, eventName, constantVersionNumber)

    }

}
