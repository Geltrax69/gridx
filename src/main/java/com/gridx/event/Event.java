package com.gridx.event;

import java.time.LocalDateTime;
import java.util.*;

public class Event {
    public final EventType type;
    public final LocalDateTime timestamp;
    public final Map<String, Object> data = new HashMap<>();

    public Event(EventType type) {
        this.type = type;
        this.timestamp = LocalDateTime.now();
    }

    public Event with(String key, Object value) {
        data.put(key, value);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) data.get(key);
    }

    @Override public String toString() { return timestamp + " [" + type + "]"; }
}
