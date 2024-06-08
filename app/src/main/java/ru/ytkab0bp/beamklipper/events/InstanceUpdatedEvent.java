package ru.ytkab0bp.beamklipper.events;

import ru.ytkab0bp.eventbus.Event;

@Event
public class InstanceUpdatedEvent {
    public final String id;

    public InstanceUpdatedEvent(String id) {
        this.id = id;
    }
}
