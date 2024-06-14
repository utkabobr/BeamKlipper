package ru.ytkab0bp.beamklipper.events;

import ru.ytkab0bp.beamklipper.KlipperInstance;
import ru.ytkab0bp.eventbus.Event;

@Event
public class WebStateChangedEvent {
    public final KlipperInstance.State state;

    public WebStateChangedEvent(KlipperInstance.State state) {
        this.state = state;
    }
}
