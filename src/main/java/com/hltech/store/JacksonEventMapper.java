package com.hltech.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JacksonEventMapper implements EventMapper {

    private final ObjectMapper objectMapper;

    public JacksonEventMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public <T extends Event> T stringToEvent(String eventString, Class<T> eventType) {
        try {
            return objectMapper.readValue(eventString, eventType);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not read event json", ex);
        }
    }

    @Override
    public <T extends Event> String eventToString(T event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not write event json", ex);
        }
    }

}
