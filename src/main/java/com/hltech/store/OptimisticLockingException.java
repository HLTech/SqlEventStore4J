package com.hltech.store;

import java.util.UUID;

public class OptimisticLockingException extends EventStoreException {

    private static final String MESSAGE_TEMPLATE = "Could not save event to database with aggregateId %s, aggregateName %s and expectedVersion %s";

    public OptimisticLockingException(UUID aggregateId, String aggregateName, int expectedAggregateVesion) {
        super(String.format(MESSAGE_TEMPLATE, aggregateId, aggregateName, expectedAggregateVesion));
    }

}
