package ru.ytkab0bp.beamklipper.events;

import ru.ytkab0bp.eventbus.Event;

@Event
public class InstanceDestroyedEvent {
    public final String id;

    public InstanceDestroyedEvent(String id) {
        this.id = id;
    }
}
