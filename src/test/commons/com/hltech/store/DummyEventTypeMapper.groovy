package com.hltech.store

class DummyEventTypeMapper implements EventTypeMapper<DummyBaseEvent> {

    @Override
    String toName(Class<? extends DummyBaseEvent> eventType) {
        "DummyEvent"
    }

    @Override
    int toVersion(Class<? extends DummyBaseEvent> eventType) {
        1
    }

    @Override
    Class<? extends DummyBaseEvent> toType(String eventName, int eventVersion) {
        DummyEvent.class
    }

    @Override
    void registerMapping(TypeNameAndVersion<? extends DummyBaseEvent> mapping) {

    }

}
