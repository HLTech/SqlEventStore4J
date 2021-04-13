package com.hltech.store.versioning;

import com.hltech.store.EventStoreException;

public class NonUniqueMappingException extends EventStoreException {

    NonUniqueMappingException(String message) {
        super(message);
    }

}
