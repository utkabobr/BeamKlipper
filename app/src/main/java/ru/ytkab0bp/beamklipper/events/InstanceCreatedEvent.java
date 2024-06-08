package ru.ytkab0bp.beamklipper.events;

import ru.ytkab0bp.eventbus.Event;

@Event
public class InstanceCreatedEvent {
    public final String id;

    public InstanceCreatedEvent(String id) {
        this.id = id;
    }
}
