package com.hltech.store

import spock.lang.Specification
import spock.lang.Subject

class SimpleEventTypeMapperUT extends Specification {

    @Subject
    def simpleEventTypeMapper = new SimpleEventTypeMapper<DummyBaseEvent>()

    def "toType should return expected type when one previously registered"() {

        given: 'EventType registered for eventName and eventVersion'
            simpleEventTypeMapper.registerMapping(eventType, eventName, eventVersion)

        expect: 'EventType found by eventName and eventVersion'
            eventType == simpleEventTypeMapper.toType(eventName, eventVersion)

    }

    def "toType should return expected type when multi type registered"() {

        given: 'EventType registered for eventName and eventVersion'
            simpleEventTypeMapper.registerMapping(eventType, eventName, eventVersion)

        and: 'Another eventType registered for another eventName and another eventVersion'
            simpleEventTypeMapper.registerMapping(anotherEventType, anotherEventName, anotherEventVersion)

        expect: 'EventType found by eventName and eventVersion'
            eventType == simpleEventTypeMapper.toType(eventName, eventVersion)

        and: 'Another eventType found by another eventName and another eventVersion'
            anotherEventType == simpleEventTypeMapper.toType(anotherEventName, anotherEventVersion)

    }

    def "toType should return expected type when multi type registered with same names but different versions"() {

        given: 'EventType registered for eventName and eventVersion'
            simpleEventTypeMapper.registerMapping(eventType, eventName, eventVersion)

        and: 'Another eventType registered for eventName and another eventVersion'
            simpleEventTypeMapper.registerMapping(anotherEventType, eventName, anotherEventVersion)

        expect: 'EventType found by eventName and eventVersion'
            eventType == simpleEventTypeMapper.toType(eventName, eventVersion)

        and: 'Another eventType found by eventName and another eventVersion'
            anotherEventType == simpleEventTypeMapper.toType(eventName, anotherEventVersion)

    }

    def "toType should return expected type when multi type registered with same versions but different names"() {

        given: 'EventType registered for eventName and eventVersion'
            simpleEventTypeMapper.registerMapping(eventType, eventName, eventVersion)

        and: 'Another eventType registered for another eventName and eventVersion'
            simpleEventTypeMapper.registerMapping(anotherEventType, anotherEventName, eventVersion)

        expect: 'EventType found by eventName and eventVersion'
            eventType == simpleEventTypeMapper.toType(eventName, eventVersion)

        and: 'Another eventType found by another eventName and eventVersion'
            anotherEventType == simpleEventTypeMapper.toType(anotherEventName, eventVersion)

    }

    def "toType should throw exception when mapping has not been previously registered"() {

        when: 'Search for eventType by eventName and eventVersion'
            simpleEventTypeMapper.toType(eventName, eventVersion)

        then: 'expected exception thrown'
            def ex = thrown(EventTypeMapper.EventTypeMappingException)
            ex.message == "Mapping to event type not found for event name: $eventName and event version: $eventVersion"

    }

    def "toType should throw exception when mapping has been registered but for another eventName"() {

        given: 'EventType registered for eventName and eventVersion'
            simpleEventTypeMapper.registerMapping(eventType, eventName, eventVersion)

        when: 'Search for eventType by another eventName'
            simpleEventTypeMapper.toType(anotherEventName, eventVersion)

        then: 'expected exception thrown'
            def ex = thrown(EventTypeMapper.EventTypeMappingException)
            ex.message == "Mapping to event type not found for event name: $anotherEventName and event version: $eventVersion"

    }

    def "toType should throw exception when mapping has been registered but for another eventVersion"() {

        given: 'EventType registered for eventName and eventVersion'
            simpleEventTypeMapper.registerMapping(eventType, eventName, eventVersion)

        when: 'Search for eventType by another eventVersion'
            simpleEventTypeMapper.toType(eventName, anotherEventVersion)

        then: 'expected exception thrown'
            def ex = thrown(EventTypeMapper.EventTypeMappingException)
            ex.message == "Mapping to event type not found for event name: $eventName and event version: $anotherEventVersion"

    }

    def "toName should return expected eventName when one previously registered"() {

        given: 'EventType registered for eventName and eventVersion'
            simpleEventTypeMapper.registerMapping(eventType, eventName, eventVersion)

        expect: 'EventName found by eventType'
            eventName == simpleEventTypeMapper.toName(eventType)

    }

    def "toName should return expected name when multi type registered"() {

        given: 'EventType registered for eventName and eventVersion'
            simpleEventTypeMapper.registerMapping(eventType, eventName, eventVersion)

        and: 'Another eventType registered for another eventName and another eventVersion'
            simpleEventTypeMapper.registerMapping(anotherEventType, anotherEventName, anotherEventVersion)

        expect: 'EventName found by eventType'
            eventName == simpleEventTypeMapper.toName(eventType)

        and: 'Another eventName found by another eventType'
            anotherEventName == simpleEventTypeMapper.toName(anotherEventType)

    }

    def "toName should return expected name when multi type registered with same names but different versions"() {

        given: 'EventType registered for eventName and eventVersion'
            simpleEventTypeMapper.registerMapping(eventType, eventName, eventVersion)

        and: 'Another eventType registered for eventName and another eventVersion'
            simpleEventTypeMapper.registerMapping(anotherEventType, eventName, anotherEventVersion)

        expect: 'EventName found by eventType'
            eventName == simpleEventTypeMapper.toName(eventType)

        and: 'EventName found by another eventType'
            eventName == simpleEventTypeMapper.toName(anotherEventType)

    }

    def "toName should throw exception when mapping has not been previously registered"() {

        when: 'Search for eventName by eventType'
            simpleEventTypeMapper.toName(eventType)

        then: 'expected exception thrown'
            def ex = thrown(EventTypeMapper.EventTypeMappingException)
            ex.message == "Mapping to event name not found for event type: $eventType"

    }

    def "toName should throw exception when mapping has been registered but for another eventType"() {

        given: 'EventType registered for eventName and eventVersion'
            simpleEventTypeMapper.registerMapping(eventType, eventName, eventVersion)

        when: 'Search for eventName by another eventType'
            simpleEventTypeMapper.toName(anotherEventType)

        then: 'expected exception thrown'
            def ex = thrown(EventTypeMapper.EventTypeMappingException)
            ex.message == "Mapping to event name not found for event type: $anotherEventType"

    }

    def "toVersion should return expected eventVersion when one previously registered"() {

        given: 'EventType registered for eventName and eventVersion'
            simpleEventTypeMapper.registerMapping(eventType, eventName, eventVersion)

        expect: 'EventVersion found by eventType'
            eventVersion == simpleEventTypeMapper.toVersion(eventType)

    }

    def "toVersion should return expected version when multi type registered"() {

        given: 'EventType registered for eventName and eventVersion'
            simpleEventTypeMapper.registerMapping(eventType, eventName, eventVersion)

        and: 'Another eventType registered for another eventName and another eventVersion'
            simpleEventTypeMapper.registerMapping(anotherEventType, anotherEventName, anotherEventVersion)

        expect: 'EventVersion found by eventType'
            eventVersion == simpleEventTypeMapper.toVersion(eventType)

        and: 'Another eventVersion found by another eventType'
            anotherEventVersion == simpleEventTypeMapper.toVersion(anotherEventType)

    }

    def "toVersion should return expected version when multi type registered with same versions but different names"() {

        given: 'EventType registered for eventName and eventVersion'
            simpleEventTypeMapper.registerMapping(eventType, eventName, eventVersion)

        and: 'Another eventType registered for another eventName and eventVersion'
            simpleEventTypeMapper.registerMapping(anotherEventType, anotherEventName, eventVersion)

        expect: 'EventVersion found by eventType'
            eventVersion == simpleEventTypeMapper.toVersion(eventType)

        and: 'EventVersion found by another eventType'
            eventVersion == simpleEventTypeMapper.toVersion(anotherEventType)

    }

    def "toVersion should throw exception when mapping has not been previously registered"() {

        when: 'Search for eventVersion by eventType'
            simpleEventTypeMapper.toVersion(eventType)

        then: 'expected exception thrown'
            def ex = thrown(EventTypeMapper.EventTypeMappingException)
            ex.message == "Mapping to event version not found for event type: $eventType"

    }

    def "toVersion should throw exception when mapping has been registered but for another eventType"() {

        given: 'EventType registered for eventName and eventVersion'
            simpleEventTypeMapper.registerMapping(eventType, eventName, eventVersion)

        when: 'Search for eventVersion by another eventType'
            simpleEventTypeMapper.toVersion(anotherEventType)

        then: 'expected exception thrown'
            def ex = thrown(EventTypeMapper.EventTypeMappingException)
            ex.message == "Mapping to event version not found for event type: $anotherEventType"

    }

    static eventType = DummyEvent.class
    static eventName = "DummyEvent"
    static eventVersion = 1 as short

    static anotherEventType = AnotherDummyEvent.class
    static anotherEventName = "AnotherDummyEvent"
    static anotherEventVersion = 2 as short

}
