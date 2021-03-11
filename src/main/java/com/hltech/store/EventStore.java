package com.hltech.store;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface EventStore<E> {

    void save(
            E event,
            String streamType
    );

    List<E> findAll(UUID aggregateId);

    Map<UUID, List<E>> findAll(String streamType);

    List<E> findAll(UUID aggregateId, String streamType);

    List<E> findAllToEvent(E toEvent, String streamType);

}
