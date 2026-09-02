package com.gridx.event;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class EventBus {
    private final Map<EventType, CopyOnWriteArrayList<Consumer<Event>>> subscribers = new ConcurrentHashMap<>();
    private final BlockingQueue<Event> eventQueue = new LinkedBlockingQueue<>(10000);
    private volatile boolean running = false;
    private Thread processorThread;

    public void subscribe(EventType type, Consumer<Event> handler) {
        subscribers.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add(handler);
    }

    public void subscribe(Set<EventType> types, Consumer<Event> handler) {
        types.forEach(t -> subscribe(t, handler));
    }

    public void publish(Event event) {
        if (!eventQueue.offer(event)) {
            System.err.println("Event queue full, dropping event: " + event.type);
        }
    }

    public void start() {
        running = true;
        processorThread = new Thread(this::processEvents, "EventBus-Processor");
        processorThread.setDaemon(true);
        processorThread.start();
    }

    public void stop() {
        running = false;
        if (processorThread != null) processorThread.interrupt();
    }

    private void processEvents() {
        while (running) {
            try {
                Event event = eventQueue.poll(100, TimeUnit.MILLISECONDS);
                if (event != null) {
                    List<Consumer<Event>> handlers = subscribers.get(event.type);
                    if (handlers != null) {
                        for (Consumer<Event> handler : handlers) {
                            try {
                                handler.accept(event);
                            } catch (Exception e) {
                                System.err.println("Handler error for " + event.type + ": " + e.getMessage());
                            }
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
