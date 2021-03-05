package com.hltech.store

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Specification
import spock.lang.Subject

class JacksonEventMapperUT extends Specification {

    ObjectMapper objectMapper = Mock()

    @Subject
    def jacksonEventMapper = new JacksonEventMapper(objectMapper)


    def "eventToString should call objectMapper.writeValueAsString method and propagate its response"() {

        given: 'Event'
            def event = new DummyEvent()

        and: 'Expected json'
            def expectedJson = "{}"

        and: 'objectMapper.writeValueAsString return expected json'
            objectMapper.writeValueAsString(event) >> expectedJson

        when: 'Map event to json'
            def actualJson = jacksonEventMapper.eventToString(event)

        then: 'Json as expected'
            actualJson == expectedJson

    }

    def "eventToString should throw exception when objectMapper.writeValueAsString throw JsonProcessingException"() {

        given: 'Event'
            def event = new DummyEvent()

        and: 'objectMapper.writeValueAsString throw JsonProcessingException'
            objectMapper.writeValueAsString(event) >> { throw new JsonProcessingException('') }

        when: 'Map event to json'
            jacksonEventMapper.eventToString(event)

        then: 'Json as expected'
            def ex = thrown(IllegalStateException)
            ex.message == 'Could not write event json'
    }

    def "stringToEvent should call objectMapper.readValue method and propagate its response"() {

        given: 'Json'
            def json = "{}"

        and: 'Expected event'
            def expectedEvent = new DummyEvent()

        and: 'objectMapper.readValue return expected event'
            objectMapper.readValue(json, DummyEvent) >> expectedEvent

        when: 'Map json to event'
            def actualEvent = jacksonEventMapper.stringToEvent(json, DummyEvent)

        then: 'Event as expected'
            actualEvent == expectedEvent

    }

    def "stringToEvent should throw exception when objectMapper.readValue throw JsonProcessingException"() {

        given: 'Json'
            def json = "{}"

        and: 'objectMapper.readValue throw JsonProcessingException'
            objectMapper.readValue(json, DummyEvent) >> { throw new JsonProcessingException('') }

        when: 'Map json to event'
            jacksonEventMapper.stringToEvent(json, DummyEvent)

        then: 'Json as expected'
            def ex = thrown(IllegalStateException)
            ex.message == 'Could not read event json'

    }

}
