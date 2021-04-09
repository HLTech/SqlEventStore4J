package com.hltech.store.versioning

import com.hltech.store.AnotherDummyEvent
import com.hltech.store.DummyBaseEvent
import com.hltech.store.DummyEvent
import spock.lang.Specification
import spock.lang.Subject

class MultipleEventVersionPolicyUT extends Specification {

    @Subject
    def eventVersionPolicy = new MultipleEventVersionPolicy<DummyBaseEvent>()

    def "toType should return expected type when one previously registered"() {

        given: 'EventType registered for eventName and eventVersion'
            eventVersionPolicy.registerMapping(eventType, eventName, eventVersion)

        expect: 'EventType found by eventName and eventVersion'
            eventType == eventVersionPolicy.toType(eventName, eventVersion)

    }

    def "toType should return expected type when multi type registered"() {

        given: 'EventType registered for eventName and eventVersion'
            eventVersionPolicy.registerMapping(eventType, eventName, eventVersion)

        and: 'Another eventType registered for another eventName and another eventVersion'
            eventVersionPolicy.registerMapping(anotherEventType, anotherEventName, anotherEventVersion)

        expect: 'EventType found by eventName and eventVersion'
            eventType == eventVersionPolicy.toType(eventName, eventVersion)

        and: 'Another eventType found by another eventName and another eventVersion'
            anotherEventType == eventVersionPolicy.toType(anotherEventName, anotherEventVersion)

    }

    def "toType should return expected type when multi type registered with same names but different versions"() {

        given: 'EventType registered for eventName and eventVersion'
            eventVersionPolicy.registerMapping(eventType, eventName, eventVersion)

        and: 'Another eventType registered for eventName and another eventVersion'
            eventVersionPolicy.registerMapping(anotherEventType, eventName, anotherEventVersion)

        expect: 'EventType found by eventName and eventVersion'
            eventType == eventVersionPolicy.toType(eventName, eventVersion)

        and: 'Another eventType found by eventName and another eventVersion'
            anotherEventType == eventVersionPolicy.toType(eventName, anotherEventVersion)

    }

    def "toType should return expected type when multi type registered with same versions but different names"() {

        given: 'EventType registered for eventName and eventVersion'
            eventVersionPolicy.registerMapping(eventType, eventName, eventVersion)

        and: 'Another eventType registered for another eventName and eventVersion'
            eventVersionPolicy.registerMapping(anotherEventType, anotherEventName, eventVersion)

        expect: 'EventType found by eventName and eventVersion'
            eventType == eventVersionPolicy.toType(eventName, eventVersion)

        and: 'Another eventType found by another eventName and eventVersion'
            anotherEventType == eventVersionPolicy.toType(anotherEventName, eventVersion)

    }

    def "toType should throw exception when mapping has not been previously registered"() {

        when: 'Search for eventType by eventName and eventVersion'
            eventVersionPolicy.toType(eventName, eventVersion)

        then: 'expected exception thrown'
            def ex = thrown(EventVersionPolicy.EventTypeMappingException)
            ex.message == "Mapping to event type not found for event name: $eventName and event version: $eventVersion"

    }

    def "toType should throw exception when mapping has been registered but for another eventName"() {

        given: 'EventType registered for eventName and eventVersion'
            eventVersionPolicy.registerMapping(eventType, eventName, eventVersion)

        when: 'Search for eventType by another eventName'
            eventVersionPolicy.toType(anotherEventName, eventVersion)

        then: 'expected exception thrown'
            def ex = thrown(EventVersionPolicy.EventTypeMappingException)
            ex.message == "Mapping to event type not found for event name: $anotherEventName and event version: $eventVersion"

    }

    def "toType should throw exception when mapping has been registered but for another eventVersion"() {

        given: 'EventType registered for eventName and eventVersion'
            eventVersionPolicy.registerMapping(eventType, eventName, eventVersion)

        when: 'Search for eventType by another eventVersion'
            eventVersionPolicy.toType(eventName, anotherEventVersion)

        then: 'expected exception thrown'
            def ex = thrown(EventVersionPolicy.EventTypeMappingException)
            ex.message == "Mapping to event type not found for event name: $eventName and event version: $anotherEventVersion"

    }

    def "toName should return expected eventName when one previously registered"() {

        given: 'EventType registered for eventName and eventVersion'
            eventVersionPolicy.registerMapping(eventType, eventName, eventVersion)

        expect: 'EventName found by eventType'
            eventName == eventVersionPolicy.toName(eventType)

    }

    def "toName should return expected name when multi type registered"() {

        given: 'EventType registered for eventName and eventVersion'
            eventVersionPolicy.registerMapping(eventType, eventName, eventVersion)

        and: 'Another eventType registered for another eventName and another eventVersion'
            eventVersionPolicy.registerMapping(anotherEventType, anotherEventName, anotherEventVersion)

        expect: 'EventName found by eventType'
            eventName == eventVersionPolicy.toName(eventType)

        and: 'Another eventName found by another eventType'
            anotherEventName == eventVersionPolicy.toName(anotherEventType)

    }

    def "toName should return expected name when multi type registered with same names but different versions"() {

        given: 'EventType registered for eventName and eventVersion'
            eventVersionPolicy.registerMapping(eventType, eventName, eventVersion)

        and: 'Another eventType registered for eventName and another eventVersion'
            eventVersionPolicy.registerMapping(anotherEventType, eventName, anotherEventVersion)

        expect: 'EventName found by eventType'
            eventName == eventVersionPolicy.toName(eventType)

        and: 'EventName found by another eventType'
            eventName == eventVersionPolicy.toName(anotherEventType)

    }

    def "toName should throw exception when mapping has not been previously registered"() {

        when: 'Search for eventName by eventType'
            eventVersionPolicy.toName(eventType)

        then: 'expected exception thrown'
            def ex = thrown(EventVersionPolicy.EventTypeMappingException)
            ex.message == "Mapping to event name not found for event type: $eventType"

    }

    def "toName should throw exception when mapping has been registered but for another eventType"() {

        given: 'EventType registered for eventName and eventVersion'
            eventVersionPolicy.registerMapping(eventType, eventName, eventVersion)

        when: 'Search for eventName by another eventType'
            eventVersionPolicy.toName(anotherEventType)

        then: 'expected exception thrown'
            def ex = thrown(EventVersionPolicy.EventTypeMappingException)
            ex.message == "Mapping to event name not found for event type: $anotherEventType"

    }

    def "toVersion should return expected eventVersion when one previously registered"() {

        given: 'EventType registered for eventName and eventVersion'
            eventVersionPolicy.registerMapping(eventType, eventName, eventVersion)

        expect: 'EventVersion found by eventType'
            eventVersion == eventVersionPolicy.toVersion(eventType)

    }

    def "toVersion should return expected version when multi type registered"() {

        given: 'EventType registered for eventName and eventVersion'
            eventVersionPolicy.registerMapping(eventType, eventName, eventVersion)

        and: 'Another eventType registered for another eventName and another eventVersion'
            eventVersionPolicy.registerMapping(anotherEventType, anotherEventName, anotherEventVersion)

        expect: 'EventVersion found by eventType'
            eventVersion == eventVersionPolicy.toVersion(eventType)

        and: 'Another eventVersion found by another eventType'
            anotherEventVersion == eventVersionPolicy.toVersion(anotherEventType)

    }

    def "toVersion should return expected version when multi type registered with same versions but different names"() {

        given: 'EventType registered for eventName and eventVersion'
            eventVersionPolicy.registerMapping(eventType, eventName, eventVersion)

        and: 'Another eventType registered for another eventName and eventVersion'
            eventVersionPolicy.registerMapping(anotherEventType, anotherEventName, eventVersion)

        expect: 'EventVersion found by eventType'
            eventVersion == eventVersionPolicy.toVersion(eventType)

        and: 'EventVersion found by another eventType'
            eventVersion == eventVersionPolicy.toVersion(anotherEventType)

    }

    def "toVersion should throw exception when mapping has not been previously registered"() {

        when: 'Search for eventVersion by eventType'
            eventVersionPolicy.toVersion(eventType)

        then: 'expected exception thrown'
            def ex = thrown(EventVersionPolicy.EventTypeMappingException)
            ex.message == "Mapping to event version not found for event type: $eventType"

    }

    def "toVersion should throw exception when mapping has been registered but for another eventType"() {

        given: 'EventType registered for eventName and eventVersion'
            eventVersionPolicy.registerMapping(eventType, eventName, eventVersion)

        when: 'Search for eventVersion by another eventType'
            eventVersionPolicy.toVersion(anotherEventType)

        then: 'expected exception thrown'
            def ex = thrown(EventVersionPolicy.EventTypeMappingException)
            ex.message == "Mapping to event version not found for event type: $anotherEventType"

    }

    def "registerMapping should throw exception when mapping has already been registered for given eventName and version"() {

        given: 'EventType registered for eventName and eventVersion'
            eventVersionPolicy.registerMapping(eventType, eventName, eventVersion)

        when: 'EventType registered for same eventName and eventVersion'
            eventVersionPolicy.registerMapping(anotherEventType, eventName, eventVersion)

        then: 'expected exception thrown'
            def ex = thrown(EventVersionPolicy.NonUniqueMappingException)
            ex.message == "Mapping for event name: $eventName and version: $eventVersion was already configured for type: $eventType"

    }

    def "registerMapping should throw exception when mapping has already been registered for given eventType"() {

        given: 'EventType registered for eventType'
            eventVersionPolicy.registerMapping(eventType, eventName, eventVersion)

        when: 'EventType registered for same eventType'
            eventVersionPolicy.registerMapping(eventType, anotherEventName, anotherEventVersion)

        then: 'expected exception thrown'
            def ex = thrown(EventVersionPolicy.NonUniqueMappingException)
            ex.message == "Mapping for event type: $eventType was already configured for event name: $eventName and version: $eventVersion"

    }

    static eventType = DummyEvent.class
    static eventName = "DummyEvent"
    static eventVersion = 1

    static anotherEventType = AnotherDummyEvent.class
    static anotherEventName = "AnotherDummyEvent"
    static anotherEventVersion = 2

}
