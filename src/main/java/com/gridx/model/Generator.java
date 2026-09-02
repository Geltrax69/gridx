package com.gridx.model;

public class Generator extends PowerProducer {
    public volatile boolean running = false;
    public double startupTimeMinutes = 5;
    public volatile long startupStartedAt = 0;

    public Generator(String id, String name, double x, double y, double maxOutput, double cost) {
        super(id, name, NodeType.GENERATOR, x, y, maxOutput, cost, 0.45);
    }

    @Override
    public synchronized void update(long currentTick) {
        if (getStatus() != NodeStatus.ACTIVE || !running) {
            currentOutput = 0;
            return;
        }
        // Generators are stable and reliable
        currentOutput = maxOutput * efficiency;
        addEnergyGenerated(currentOutput);
    }

    public synchronized void start() {
        running = true;
        startupStartedAt = System.currentTimeMillis();
    }

    public synchronized void stop() {
        running = false;
        currentOutput = 0;
    }
}
