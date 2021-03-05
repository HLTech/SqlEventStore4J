package com.hltech.store

class DummyAggregate {

    static INITIAL_STATE_SUPPLIER = { -> new DummyAggregate() }
    static EVENT_APPLIER = { DummyAggregate aggregate, Event event -> aggregate.apply(event) }

    List<Event> appliedEvents = []

    DummyAggregate apply(Event event) {
        appliedEvents.add(event)
        this
    }

}
