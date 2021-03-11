package com.hltech.store

class DummyEventTypeMapper implements EventTypeMapper<DummyBaseEvent> {

    @Override
    String toName(Class<? extends DummyBaseEvent> eventType) {
        "DummyEvent"
    }

    @Override
    short toVersion(Class<? extends DummyBaseEvent> eventType) {
        1
    }

    @Override
    Class<? extends DummyBaseEvent> toType(String eventName, short eventVersion) {
        DummyEvent.class
    }

    @Override
    void registerMapping(TypeNameAndVersion<? extends DummyBaseEvent> mapping) {

    }

}
