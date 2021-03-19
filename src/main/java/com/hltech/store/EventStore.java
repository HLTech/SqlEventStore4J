package com.hltech.store;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface EventStore<E> {

    void save(
            E event,
            String streamName
    );

    Map<UUID, List<E>> findAll(String streamName);

    List<E> findAll(UUID aggregateId, String streamName);

    List<E> findAllToEvent(E toEvent, String streamName);

}
