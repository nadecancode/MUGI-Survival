package me.allen.survival.event;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancellableEvent extends BaseEvent {
    private boolean cancelled;
}
