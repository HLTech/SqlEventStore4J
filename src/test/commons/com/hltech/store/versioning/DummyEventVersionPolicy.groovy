package com.hltech.store.versioning

import com.hltech.store.DummyBaseEvent
import com.hltech.store.DummyEvent

class DummyEventVersionPolicy implements EventVersionPolicy<DummyBaseEvent> {

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

}
