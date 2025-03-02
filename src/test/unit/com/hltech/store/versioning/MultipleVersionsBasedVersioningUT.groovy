package com.hltech.store.versioning

import com.hltech.store.AnotherDummyEvent
import com.hltech.store.DummyBaseEvent
import com.hltech.store.DummyEvent
import com.hltech.store.InvalidDummyEvent
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class MultipleVersionsBasedVersioningUT extends Specification {

    @Subject
    def eventVersioningStrategy = new MultipleVersionsBasedVersioning<DummyBaseEvent>()

    def "toEvent should return expected event when one previously registered"() {

        given: 'EventType registered for eventName'
            eventVersioningStrategy.registerEvent(eventType, eventName, eventVersion)

        expect: 'toEvent return expected event'
            event == eventVersioningStrategy.toEvent(eventJson, eventName, eventVersion)

    }

    def "toEvent should return expected event when multi type registered"() {

        given: 'EventType registered for eventName'
            eventVersioningStrategy.registerEvent(eventType, eventName, eventVersion)

        and: 'Another eventType registered for another eventName'
            eventVersioningStrategy.registerEvent(anotherEventType, anotherEventName, anotherEventVersion)

        expect: 'toEvent return expected event for eventName'
            event == eventVersioningStrategy.toEvent(eventJson, eventName, eventVersion)

        and: 'toEvent return expected event for anotherEventName'
            anotherEvent == eventVersioningStrategy.toEvent(anotherEventJson, anotherEventName, anotherEventVersion)

    }

    def "toEvent should return expected event when multi type registered with same names but different versions"() {

        given: 'EventType registered for eventName and eventVersion'
            eventVersioningStrategy.registerEvent(eventType, eventName, eventVersion)

        and: 'Another eventType registered for eventName and another eventVersion'
            eventVersioningStrategy.registerEvent(anotherEventType, eventName, anotherEventVersion)

        expect: 'toEvent return expected event for eventName and eventVersion'
            event == eventVersioningStrategy.toEvent(eventJson, eventName, eventVersion)

        and: 'toEvent return expected event for eventName and another eventVersion'
            anotherEvent == eventVersioningStrategy.toEvent(anotherEventJson, eventName, anotherEventVersion)

    }

    def "toEvent should return expected event when multi type registered with same versions but different names"() {

        given: 'EventType registered for eventName and eventVersion'
            eventVersioningStrategy.registerEvent(eventType, eventName, eventVersion)

        and: 'Another eventType registered for another eventName and eventVersion'
            eventVersioningStrategy.registerEvent(anotherEventType, anotherEventName, eventVersion)

        expect: 'toEvent return expected event for eventName and eventVersion'
            event == eventVersioningStrategy.toEvent(eventJson, eventName, eventVersion)

        and: 'toEvent return expected event for another eventName and eventVersion'
            anotherEvent == eventVersioningStrategy.toEvent(anotherEventJson, anotherEventName, eventVersion)

    }

    def "toEvent should throw exception when there is additional attribute in json"() {

        given: 'EventType registered for eventName'
            eventVersioningStrategy.registerEvent(eventType, eventName, eventVersion)

        when: 'Try to get event from json with additional attribute'
            eventVersioningStrategy.toEvent(eventJsonWithAdditionalAttribute, eventName, eventVersion)

        then: 'expected exception thrown'
            def ex = thrown(EventBodyMappingException)
            ex.message == "Could not create event of type com.hltech.store.DummyEvent from json $eventJsonWithAdditionalAttribute"

    }

    def "toEvent should throw exception when one of attribute is not present in json"() {

        given: 'EventType registered for eventName'
            eventVersioningStrategy.registerEvent(eventType, eventName, eventVersion)

        when: 'Try to get event from json without one of attribute'
            eventVersioningStrategy.toEvent(eventJsonWithoutOneOfAttribute, eventName, eventVersion)

        then: 'expected exception thrown'
            def ex = thrown(EventBodyMappingException)
            ex.message == "Could not create event of type com.hltech.store.DummyEvent from json $eventJsonWithoutOneOfAttribute"

    }

    def "toEvent should throw exception when mapping has not been previously registered"() {

        when: 'Try to get event for unregistered eventType'
            eventVersioningStrategy.toEvent(eventJson, eventName, eventVersion)

        then: 'expected exception thrown'
            def ex = thrown(EventTypeMappingException)
            ex.message == "Mapping to event type not found for event name: $eventName and event version: $eventVersion"

    }

    def "toEvent should throw exception when mapping has been registered but for another eventName"() {

        given: 'EventType registered for eventName and eventVersion'
            eventVersioningStrategy.registerEvent(eventType, eventName, eventVersion)

        when: 'Try to get event by another eventName'
            eventVersioningStrategy.toEvent(eventJson, anotherEventName, eventVersion)

        then: 'expected exception thrown'
            def ex = thrown(EventTypeMappingException)
            ex.message == "Mapping to event type not found for event name: $anotherEventName and event version: $eventVersion"

    }

    def "toEvent should throw exception when mapping has been registered but for another eventVersion"() {

        given: 'EventType registered for eventName and eventVersion'
            eventVersioningStrategy.registerEvent(eventType, eventName, eventVersion)

        when: 'Try to get event by another eventVersion'
            eventVersioningStrategy.toEvent(eventJson, eventName, anotherEventVersion)

        then: 'expected exception thrown'
            def ex = thrown(EventTypeMappingException)
            ex.message == "Mapping to event type not found for event name: $eventName and event version: $anotherEventVersion"

    }

    @Unroll
    def "toEvent should return event that has previously been used to create json"() {

        given: 'EventType registered for eventName'
            eventVersioningStrategy.registerEvent(eventType, eventName, eventVersion)

        and: 'Json created for event'
            def json = eventVersioningStrategy.toJson(givenEvent)

        expect: 'toEvent return event that has been previously used to create json'
            givenEvent == eventVersioningStrategy.toEvent(json, eventName, eventVersion)

        where:
            givenEvent << [
                    event,
                    event.withOptionalAttribute("value")
            ]

    }

    def "toName should return expected eventName when one previously registered"() {

        given: 'EventType registered for eventName and eventVersion'
            eventVersioningStrategy.registerEvent(eventType, eventName, eventVersion)

        expect: 'EventName found by eventType'
            eventName == eventVersioningStrategy.toName(eventType)

    }

    def "toName should return expected name when multi type registered"() {

        given: 'EventType registered for eventName and eventVersion'
            eventVersioningStrategy.registerEvent(eventType, eventName, eventVersion)

        and: 'Another eventType registered for another eventName and another eventVersion'
            eventVersioningStrategy.registerEvent(anotherEventType, anotherEventName, anotherEventVersion)

        expect: 'EventName found by eventType'
            eventName == eventVersioningStrategy.toName(eventType)

        and: 'Another eventName found by another eventType'
            anotherEventName == eventVersioningStrategy.toName(anotherEventType)

    }

    def "toName should return expected name when multi type registered with same names but different versions"() {

        given: 'EventType registered for eventName and eventVersion'
            eventVersioningStrategy.registerEvent(eventType, eventName, eventVersion)

        and: 'Another eventType registered for eventName and another eventVersion'
            eventVersioningStrategy.registerEvent(anotherEventType, eventName, anotherEventVersion)

        expect: 'EventName found by eventType'
            eventName == eventVersioningStrategy.toName(eventType)

        and: 'EventName found by another eventType'
            eventName == eventVersioningStrategy.toName(anotherEventType)

    }

    def "toName should throw exception when mapping has not been previously registered"() {

        when: 'Search for eventName by eventType'
            eventVersioningStrategy.toName(eventType)

        then: 'expected exception thrown'
            def ex = thrown(EventTypeMappingException)
            ex.message == "Mapping to event name not found for event type: $eventType"

    }

    def "toName should throw exception when mapping has been registered but for another eventType"() {

        given: 'EventType registered for eventName and eventVersion'
            eventVersioningStrategy.registerEvent(eventType, eventName, eventVersion)

        when: 'Search for eventName by another eventType'
            eventVersioningStrategy.toName(anotherEventType)

        then: 'expected exception thrown'
            def ex = thrown(EventTypeMappingException)
            ex.message == "Mapping to event name not found for event type: $anotherEventType"

    }

    def "toVersion should return expected eventVersion when one previously registered"() {

        given: 'EventType registered for eventName and eventVersion'
            eventVersioningStrategy.registerEvent(eventType, eventName, eventVersion)

        expect: 'EventVersion found by eventType'
            eventVersion == eventVersioningStrategy.toVersion(eventType)

    }

    def "toVersion should return expected version when multi type registered"() {

        given: 'EventType registered for eventName and eventVersion'
            eventVersioningStrategy.registerEvent(eventType, eventName, eventVersion)

        and: 'Another eventType registered for another eventName and another eventVersion'
            eventVersioningStrategy.registerEvent(anotherEventType, anotherEventName, anotherEventVersion)

        expect: 'EventVersion found by eventType'
            eventVersion == eventVersioningStrategy.toVersion(eventType)

        and: 'Another eventVersion found by another eventType'
            anotherEventVersion == eventVersioningStrategy.toVersion(anotherEventType)

    }

    def "toVersion should return expected version when multi type registered with same versions but different names"() {

        given: 'EventType registered for eventName and eventVersion'
            eventVersioningStrategy.registerEvent(eventType, eventName, eventVersion)

        and: 'Another eventType registered for another eventName and eventVersion'
            eventVersioningStrategy.registerEvent(anotherEventType, anotherEventName, eventVersion)

        expect: 'EventVersion found by eventType'
            eventVersion == eventVersioningStrategy.toVersion(eventType)

        and: 'EventVersion found by another eventType'
            eventVersion == eventVersioningStrategy.toVersion(anotherEventType)

    }

    def "toVersion should throw exception when mapping has not been previously registered"() {

        when: 'Search for eventVersion by eventType'
            eventVersioningStrategy.toVersion(eventType)

        then: 'expected exception thrown'
            def ex = thrown(EventTypeMappingException)
            ex.message == "Mapping to event version not found for event type: $eventType"

    }

    def "toVersion should throw exception when mapping has been registered but for another eventType"() {

        given: 'EventType registered for eventName and eventVersion'
            eventVersioningStrategy.registerEvent(eventType, eventName, eventVersion)

        when: 'Search for eventVersion by another eventType'
            eventVersioningStrategy.toVersion(anotherEventType)

        then: 'expected exception thrown'
            def ex = thrown(EventTypeMappingException)
            ex.message == "Mapping to event version not found for event type: $anotherEventType"

    }

    def "registerMapping should throw exception when mapping has already been registered for given eventName and version"() {

        given: 'EventType registered for eventName and eventVersion'
            eventVersioningStrategy.registerEvent(eventType, eventName, eventVersion)

        when: 'EventType registered for same eventName and eventVersion'
            eventVersioningStrategy.registerEvent(anotherEventType, eventName, eventVersion)

        then: 'expected exception thrown'
            def ex = thrown(NonUniqueMappingException)
            ex.message == "Mapping for event name: $eventName and version: $eventVersion was already configured for type: $eventType"

    }

    def "registerMapping should throw exception when mapping has already been registered for given eventType"() {

        given: 'EventType registered for eventType'
            eventVersioningStrategy.registerEvent(eventType, eventName, eventVersion)

        when: 'EventType registered for same eventType'
            eventVersioningStrategy.registerEvent(eventType, anotherEventName, anotherEventVersion)

        then: 'expected exception thrown'
            def ex = thrown(NonUniqueMappingException)
            ex.message == "Mapping for event type: $eventType was already configured for event name: $eventName and version: $eventVersion"

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
    static eventVersion = 1
    static eventJson = """{"id":"$event.id","aggregateId":"$event.aggregateId","optionalAttribute":null}"""
    static eventJsonWithAdditionalAttribute = """{"id":"$event.id","aggregateId":"$event.aggregateId", "additionalAtribute": "value"}"""
    static eventJsonWithoutOneOfAttribute = """{"id":"$event.id"}"""

    static anotherEvent = new AnotherDummyEvent()
    static anotherEventType = AnotherDummyEvent.class
    static anotherEventName = "AnotherDummyEvent"
    static anotherEventVersion = 2
    static anotherEventJson = """{"id":"$anotherEvent.id","aggregateId":"$anotherEvent.aggregateId"}"""

}
