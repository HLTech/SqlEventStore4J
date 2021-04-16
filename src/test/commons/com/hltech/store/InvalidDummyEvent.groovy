package com.hltech.store

class InvalidDummyEvent implements DummyBaseEvent {

    @Override
    UUID getId() {
        throw new RuntimeException("Could not get event id")
    }

    @Override
    UUID getAggregateId() {
        throw new RuntimeException("Could not get aggregate id")
    }

}
