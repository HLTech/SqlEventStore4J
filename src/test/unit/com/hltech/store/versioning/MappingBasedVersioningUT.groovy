package com.hltech.store.versioning

import com.hltech.store.AnotherDummyEvent
import com.hltech.store.DummyBaseEvent
import com.hltech.store.DummyEvent
import spock.lang.Specification
import spock.lang.Subject

class MappingBasedVersioningUT extends Specification {

    @Subject
    def eventVersioningStrategy = new MappingBasedVersioning<DummyBaseEvent>()

    def "toType should return expected type when one previously registered"() {

        given: 'EventType registered for eventName'
            eventVersioningStrategy.registerMapping(eventType, eventName)

        expect: 'EventType found by eventName'
            eventType == eventVersioningStrategy.toType(eventName, constantVersionNumber)

    }

    def "toType should return expected type when multi type registered"() {

        given: 'EventType registered for eventName'
            eventVersioningStrategy.registerMapping(eventType, eventName)

        and: 'Another eventType registered for another eventName'
            eventVersioningStrategy.registerMapping(anotherEventType, anotherEventName)

        expect: 'EventType found by eventName'
            eventType == eventVersioningStrategy.toType(eventName, constantVersionNumber)

        and: 'Another eventType found by another eventName'
            anotherEventType == eventVersioningStrategy.toType(anotherEventName, constantVersionNumber)

    }

    def "toType should throw exception when mapping has not been previously registered"() {

        when: 'Search for eventType by eventName'
            eventVersioningStrategy.toType(eventName, constantVersionNumber)

        then: 'expected exception thrown'
            def ex = thrown(EventVersioningStrategy.EventTypeMappingException)
            ex.message == "Mapping to event type not found for event name: $eventName"

    }

    def "toType should throw exception when mapping has been registered but for another eventName"() {

        given: 'EventType registered for eventName'
            eventVersioningStrategy.registerMapping(eventType, eventName)

        when: 'Search for eventType by another eventName'
            eventVersioningStrategy.toType(anotherEventName, constantVersionNumber)

        then: 'expected exception thrown'
            def ex = thrown(EventVersioningStrategy.EventTypeMappingException)
            ex.message == "Mapping to event type not found for event name: $anotherEventName"

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
            def ex = thrown(EventVersioningStrategy.EventTypeMappingException)
            ex.message == "Mapping to event name not found for event type: $eventType"

    }

    def "toName should throw exception when mapping has been registered but for another eventType"() {

        given: 'EventType registered for eventName'
            eventVersioningStrategy.registerMapping(eventType, eventName)

        when: 'Search for eventName by another eventType'
            eventVersioningStrategy.toName(anotherEventType)

        then: 'expected exception thrown'
            def ex = thrown(EventVersioningStrategy.EventTypeMappingException)
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
            def ex = thrown(EventVersioningStrategy.NonUniqueMappingException)
            ex.message == "Mapping for event name: $eventName was already configured for type: $eventType"

    }

    def "registerMapping should throw exception when mapping has already been registered for given eventType"() {

        given: 'EventType registered for eventType'
            eventVersioningStrategy.registerMapping(eventType, eventName)

        when: 'EventType registered for same eventType'
            eventVersioningStrategy.registerMapping(eventType, anotherEventName)

        then: 'expected exception thrown'
            def ex = thrown(EventVersioningStrategy.NonUniqueMappingException)
            ex.message == "Mapping for event type: $eventType was already configured for event name: $eventName"

    }

    def

    static eventType = DummyEvent.class
    static eventName = "DummyEvent"

    static anotherEventType = AnotherDummyEvent.class
    static anotherEventName = "AnotherDummyEvent"

    static constantVersionNumber = 1

}
