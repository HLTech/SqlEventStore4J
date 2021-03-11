package com.hltech.store;

import java.util.List;
import java.util.UUID;

public interface EventStore {

    void save(
            Event event,
            String streamType
    );

    List<Event> findAll(UUID aggregateId);

    List<Event> findAll(String streamType);

    List<Event> findAll(UUID aggregateId, String streamType);

    List<Event> findAllToEvent(Event toEvent, String streamType);

}
