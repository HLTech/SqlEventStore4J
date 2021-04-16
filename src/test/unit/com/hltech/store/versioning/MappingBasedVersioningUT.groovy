package com.hltech.store.versioning

import com.hltech.store.AnotherDummyEvent
import com.hltech.store.DummyBaseEvent
import com.hltech.store.DummyEvent
import com.hltech.store.InvalidDummyEvent
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class MappingBasedVersioningUT extends Specification {

    @Subject
    def eventVersioningStrategy = new MappingBasedVersioning<DummyBaseEvent>()

    def "toEvent should return expected event when one previously registered"() {

        given: 'EventType registered for eventName'
            eventVersioningStrategy.registerMapping(eventType, eventName)

        expect: 'toEvent return expected event'
            event == eventVersioningStrategy.toEvent(eventJson, eventName, constantVersionNumber)

    }

    def "toEvent should return expected event when multi type registered"() {

        given: 'EventType registered for eventName'
            eventVersioningStrategy.registerMapping(eventType, eventName)

        and: 'Another eventType registered for another eventName'
            eventVersioningStrategy.registerMapping(anotherEventType, anotherEventName)

        expect: 'toEvent return expected event for eventName'
            event == eventVersioningStrategy.toEvent(eventJson, eventName, constantVersionNumber)

        and: 'toEvent return expected event for anotherEventName'
            anotherEvent == eventVersioningStrategy.toEvent(anotherEventJson, anotherEventName, constantVersionNumber)

    }

    def "toEvent should return expected event when there is additional attribute in json"() {

        given: 'EventType registered for eventName'
            eventVersioningStrategy.registerMapping(eventType, eventName)

        expect: 'toEvent return expected event'
            event == eventVersioningStrategy.toEvent(eventJsonWithAdditionalAttribute, eventName, constantVersionNumber)

    }

    def "toEvent should return expected event when one of attribute is not present in json"() {

        given: 'EventType registered for eventName'
            eventVersioningStrategy.registerMapping(eventType, eventName)

        expect: 'toEvent return expected event'
            event.id == eventVersioningStrategy.toEvent(eventJsonWithoutOneOfAttribute, eventName, constantVersionNumber).id

    }

    def "toEvent should throw exception when mapping has not been previously registered"() {

        when: 'Try to get event for unregistered eventType'
            eventVersioningStrategy.toEvent(eventJson, eventName, constantVersionNumber)

        then: 'expected exception thrown'
            def ex = thrown(EventTypeMappingException)
            ex.message == "Mapping to event type not found for event name: $eventName"

    }

    def "toEvent should throw exception when mapping has been registered but for another eventName"() {

        given: 'EventType registered for eventName'
            eventVersioningStrategy.registerMapping(eventType, eventName)

        when: 'Try to get event by another eventName'
            eventVersioningStrategy.toEvent(anotherEventJson, anotherEventName, constantVersionNumber)

        then: 'expected exception thrown'
            def ex = thrown(EventTypeMappingException)
            ex.message == "Mapping to event type not found for event name: $anotherEventName"

    }

    def "toEvent should throw exception when json is invalid"() {

        given: 'EventType registered for eventName'
            eventVersioningStrategy.registerMapping(eventType, eventName)

        when: 'Try to get event by another eventName'
            eventVersioningStrategy.toEvent(invalidJson, eventName, constantVersionNumber)

        then: 'expected exception thrown'
            def ex = thrown(EventBodyMappingException)
            ex.message == "Could not create event of type com.hltech.store.DummyEvent from json $invalidJson"

    }

    @Unroll
    def "toEvent should return event that has previously been used to create json"() {

        given: 'EventType registered for eventName'
            eventVersioningStrategy.registerMapping(eventType, eventName)

        and: 'Json created for event'
            def json = eventVersioningStrategy.toJson(givenEvent)

        expect: 'toEvent return event that has been previously used to create json'
            givenEvent == eventVersioningStrategy.toEvent(json, eventName, constantVersionNumber)

        where:
            givenEvent << [
                    event,
                    event.withOptionalAttribute("value")
            ]

    }

    def "toName should return expected eventName when one previously registered"() {

        given: 'EventType registered for eventName'
            eventVersioningStrategy.registerMapping(eventType, eventName)

        expect: 'EventName found by eventType'
            eventName == eventVersioningStrategy.toName(eventType)

    }

    def "toName should return expected name when multi type registered"() {

        given: 'EventType registered for eventName'
            eventVersioningStrategy.registerMapping(eventType, eventName)

        and: 'Another eventType registered for another eventName'
            eventVersioningStrategy.registerMapping(anotherEventType, anotherEventName)

        expect: 'EventName found by eventType'
            eventName == eventVersioningStrategy.toName(eventType)

        and: 'Another eventName found by another eventType'
            anotherEventName == eventVersioningStrategy.toName(anotherEventType)

    }

    def "toName should throw exception when mapping has not been previously registered"() {

        when: 'Search for eventName by eventType'
            eventVersioningStrategy.toName(eventType)

        then: 'expected exception thrown'
            def ex = thrown(EventTypeMappingException)
            ex.message == "Mapping to event name not found for event type: $eventType"

    }

    def "toName should throw exception when mapping has been registered but for another eventType"() {

        given: 'EventType registered for eventName'
            eventVersioningStrategy.registerMapping(eventType, eventName)

        when: 'Search for eventName by another eventType'
            eventVersioningStrategy.toName(anotherEventType)

        then: 'expected exception thrown'
            def ex = thrown(EventTypeMappingException)
            ex.message == "Mapping to event name not found for event type: $anotherEventType"

    }

    def "toVersion should return constant version number"() {

        expect: 'EventVersion is equals to constant version number'
            constantVersionNumber == eventVersioningStrategy.toVersion(eventType)

    }

    def "registerMapping should throw exception when mapping has already been registered for given eventName and version"() {

        given: 'EventType registered for eventName'
            eventVersioningStrategy.registerMapping(eventType, eventName)

        when: 'EventType registered for same eventName'
            eventVersioningStrategy.registerMapping(anotherEventType, eventName)

        then: 'expected exception thrown'
            def ex = thrown(NonUniqueMappingException)
            ex.message == "Mapping for event name: $eventName was already configured for type: $eventType"

    }

    def "registerMapping should throw exception when mapping has already been registered for given eventType"() {

        given: 'EventType registered for eventType'
            eventVersioningStrategy.registerMapping(eventType, eventName)

        when: 'EventType registered for same eventType'
            eventVersioningStrategy.registerMapping(eventType, anotherEventName)

        then: 'expected exception thrown'
            def ex = thrown(NonUniqueMappingException)
            ex.message == "Mapping for event type: $eventType was already configured for event name: $eventName"

    }

    def "toJson should return valid json for event"() {

        expect: 'toJson return valid json for event'
            eventJson == eventVersioningStrategy.toJson(event)

    }

    def "toJson should throw exception when json could not be created from event"() {

        when: 'toJson return valid json for event'
            eventVersioningStrategy.toJson(new InvalidDummyEvent())

        then: 'Exception thrown'
            def ex = thrown(EventBodyMappingException)
            ex.message.contains("Could not create json from event com.hltech.store.InvalidDummyEvent")

    }

    static event = new DummyEvent()
    static eventType = DummyEvent.class
    static eventName = "DummyEvent"
    static eventJson = """{"id":"$event.id","aggregateId":"$event.aggregateId","optionalAttribute":null}"""
    static eventJsonWithAdditionalAttribute = """{"id":"$event.id","aggregateId":"$event.aggregateId", "additionalAtribute": "value"}"""
    static eventJsonWithoutOneOfAttribute = """{"id":"$event.id"}"""

    static anotherEvent = new AnotherDummyEvent()
    static anotherEventType = AnotherDummyEvent.class
    static anotherEventName = "AnotherDummyEvent"
    static anotherEventJson = """{"id":"$anotherEvent.id","aggregateId":"$anotherEvent.aggregateId"}"""

    static constantVersionNumber = 1
    static invalidJson = "{id}"

}
