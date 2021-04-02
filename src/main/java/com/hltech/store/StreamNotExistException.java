package com.hltech.store;

import java.util.UUID;

public class StreamNotExistException extends EventStoreException {

    private static final String MESSAGE_TEMPLATE = "Could not save event because stream does not exist from aggregateId %s and aggregateName %s";

    StreamNotExistException(UUID aggregateId, String aggregateName) {
        super(String.format(MESSAGE_TEMPLATE, aggregateId, aggregateName));
    }

}
