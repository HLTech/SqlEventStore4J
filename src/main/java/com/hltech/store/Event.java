package com.hltech.store;

import java.util.UUID;

interface Event {

    UUID getId();

    UUID getAggregateId();

}
