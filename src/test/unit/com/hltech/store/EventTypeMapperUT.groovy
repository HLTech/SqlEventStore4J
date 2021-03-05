package com.hltech.store

import spock.lang.Specification
import spock.lang.Subject

class EventTypeMapperUT extends Specification {

    @Subject
    EventTypeMapper eventTypeMapper = Spy()

    def "registerMapping called with eventType, eventName and eventVersion should delegate call to registerMapping with TypeNameAndVersion parameter"() {

        given: 'registerMapping call delegated'
            1 * eventTypeMapper.registerMapping(_ as EventTypeMapper.TypeNameAndVersion) >> {
                EventTypeMapper.TypeNameAndVersion typeNameAndVersion ->
                    assert typeNameAndVersion.type == eventType
                    assert typeNameAndVersion.name == eventName
                    assert typeNameAndVersion.version == eventVersion
            }

        expect: 'registerMapping called'
            eventTypeMapper.registerMapping(eventType, eventName, eventVersion)

    }

    def "registerMappings should delegate call to registerMapping with TypeNameAndVersion parameter"() {

        given: 'TypeNameAndVersion'
            def typeNameAndVersion = new EventTypeMapper.TypeNameAndVersion(eventType, eventName, eventVersion)

        and: 'registerMapping call delegated'
            1 * eventTypeMapper.registerMapping(typeNameAndVersion) >> {}

        expect: 'registerMappings called'
            eventTypeMapper.registerMappings([typeNameAndVersion])

    }

    static eventType = DummyEvent.class
    static eventName = "DummyEvent"
    static eventVersion = 1 as short

}
