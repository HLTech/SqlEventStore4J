package com.hltech.store

class DummyAggregate {

    static INITIAL_STATE_SUPPLIER = { -> new DummyAggregate() }
    static EVENT_APPLIER = { DummyAggregate aggregate, DummyBaseEvent event -> aggregate.apply(event) }
    static VERSION_APPLIER = { DummyAggregate aggregate, Integer version -> aggregate.applyVersion(version) }

    List<DummyBaseEvent> appliedEvents = []
    Integer version

    DummyAggregate apply(DummyBaseEvent event) {
        appliedEvents.add(event)
        this
    }

    DummyAggregate applyVersion(Integer version) {
        this.version = version
        this
    }

}
