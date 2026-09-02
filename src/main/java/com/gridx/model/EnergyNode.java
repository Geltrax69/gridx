package com.gridx.model;

import java.util.*;
import java.util.concurrent.*;

public abstract class EnergyNode {
    public final String id;
    public final String name;
    public final NodeType type;
    public final double x, y;
    private final List<TransmissionLine> connections = new CopyOnWriteArrayList<>();
    private volatile NodeStatus status = NodeStatus.ACTIVE;
    private volatile double currentLoad = 0;
    private volatile double maxCapacity;
    private volatile double energyGenerated = 0;
    private volatile double energyConsumed = 0;
    private volatile long lastUpdateTime;

    public EnergyNode(String id, String name, NodeType type, double x, double y, double maxCapacity) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.x = x;
        this.y = y;
        this.maxCapacity = maxCapacity;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    public abstract void update(long currentTick);

    public List<TransmissionLine> getConnections() { return new ArrayList<>(connections); }
    public void addConnection(TransmissionLine line) { connections.add(line); }

    public synchronized double getCurrentLoad() { return currentLoad; }
    public synchronized void setCurrentLoad(double load) { this.currentLoad = Math.max(0, Math.min(load, maxCapacity)); }
    public synchronized double getLoadPercentage() { return maxCapacity > 0 ? (currentLoad / maxCapacity) * 100 : 0; }
    public synchronized NodeStatus getStatus() { return status; }
    public synchronized void setStatus(NodeStatus status) { this.status = status; }
    public synchronized double getEnergyGenerated() { return energyGenerated; }
    public synchronized double getEnergyConsumed() { return energyConsumed; }
    public synchronized void addEnergyGenerated(double mw) { this.energyGenerated += mw; }
    public synchronized void addEnergyConsumed(double mw) { this.energyConsumed += mw; }
    public double getMaxCapacity() { return maxCapacity; }
    public void setMaxCapacity(double maxCapacity) { this.maxCapacity = maxCapacity; }
    public long getLastUpdateTime() { return lastUpdateTime; }
    public void updateTimestamp() { this.lastUpdateTime = System.currentTimeMillis(); }

    public double distanceTo(EnergyNode other) {
        double dx = x - other.x;
        double dy = y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    @Override public String toString() { return id + " (" + name + ") [" + type + "]"; }
    @Override public boolean equals(Object o) { return o instanceof EnergyNode n && id.equals(n.id); }
    @Override public int hashCode() { return id.hashCode(); }
}
