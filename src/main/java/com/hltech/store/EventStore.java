package com.hltech.store;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface EventStore<E> {

    void save(
            E event,
            String aggregateName
    );

    Map<UUID, List<E>> findAll(String aggregateName);

    List<E> findAll(UUID aggregateId, String aggregateName);

    List<E> findAllToEvent(E toEvent, String aggregateName);

}
