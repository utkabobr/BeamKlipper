package ru.ytkab0bp.beamklipper.events;

import ru.ytkab0bp.eventbus.Event;

@Event
public class CloudNeedQREvent {
    public final String link;

    public CloudNeedQREvent(String link) {
        this.link = link;
    }
}
