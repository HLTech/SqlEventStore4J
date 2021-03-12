package com.hltech.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JacksonEventBodyMapper<E> implements EventBodyMapper<E> {

    private final ObjectMapper objectMapper;

    public JacksonEventBodyMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public E stringToEvent(String eventString, Class<? extends E> eventType) {
        try {
            return objectMapper.readValue(eventString, eventType);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not read event json", ex);
        }
    }

    @Override
    public String eventToString(E event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not write event json", ex);
        }
    }

}
