package com.hltech.store;

public interface EventMapper<T> {

    T stringToEvent(String eventString, Class<? extends T> eventType);

    String eventToString(T event);

}
