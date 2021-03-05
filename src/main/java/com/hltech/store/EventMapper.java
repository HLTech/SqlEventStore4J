package com.hltech.store;

public interface EventMapper {

    <T extends Event> T stringToEvent(String eventString, Class<T> eventType);

    <T extends Event> String eventToString(T event);

}
