package com.hltech.store

class DummyEventTypeMapper implements EventTypeMapper {

    @Override
    <T extends Event> String toName(Class<T> eventType) {
        "DummyEvent"
    }

    @Override
    <T extends Event> short toVersion(Class<T> eventType) {
        1
    }

    @Override
    Class<? extends Event> toType(String eventName, short eventVersion) {
        DummyEvent.class
    }

    @Override
    <T extends Event> void registerMapping(TypeNameAndVersion mapping) {

    }

}
