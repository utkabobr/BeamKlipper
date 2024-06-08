package ru.ytkab0bp.beamklipper.events;

import ru.ytkab0bp.beamklipper.KlipperInstance;
import ru.ytkab0bp.eventbus.Event;

@Event
public class InstanceStateChangedEvent {
    public final String id;
    public final KlipperInstance.State state;

    public InstanceStateChangedEvent(String id, KlipperInstance.State state) {
        this.id = id;
        this.state = state;
    }
}
