package com.gridx.model;

import java.util.concurrent.atomic.*;

public class TransmissionLine {
    public final String id;
    public final EnergyNode from;
    public final EnergyNode to;
    public final double capacity; // MW
    public final double resistance; // Ohms
    public final double length; // km
    private volatile double currentFlow = 0; // MW
    private volatile double energyLoss = 0;
    private volatile LineStatus status = LineStatus.ACTIVE;
    private volatile double congestionLevel = 0;

    public TransmissionLine(String id, EnergyNode from, EnergyNode to, double capacity, double resistance, double length) {
        this.id = id;
        this.from = from;
        this.to = to;
        this.capacity = capacity;
        this.resistance = resistance;
        this.length = length;
    }

    public synchronized double getCurrentFlow() { return currentFlow; }
    public synchronized void setCurrentFlow(double flow) {
        this.currentFlow = flow;
        updateEnergyLoss();
        updateCongestion();
    }
    public synchronized double addFlow(double delta) {
        double newFlow = currentFlow + delta;
        setCurrentFlow(newFlow);
        return currentFlow;
    }
    public synchronized LineStatus getStatus() { return status; }
    public synchronized void setStatus(LineStatus status) { this.status = status; }
    public synchronized boolean isActive() { return status == LineStatus.ACTIVE; }
    public synchronized double getCongestionLevel() { return congestionLevel; }
    public synchronized double getCongestionPercentage() { return (currentFlow / capacity) * 100; }
    public synchronized double getAvailableCapacity() { return capacity - currentFlow; }
    public synchronized boolean isOverloaded() { return currentFlow > capacity; }
    public synchronized double getEnergyLoss() { return energyLoss; }

    private void updateEnergyLoss() {
        // P_loss = I^2 * R, where I = flow / voltage (simplified)
        // Energy loss = (flow^2 * resistance * length) / 1000 (kW to MW conversion)
        this.energyLoss = (currentFlow * currentFlow * resistance * length) / 10000;
    }

    private void updateCongestion() {
        if (capacity > 0) {
            this.congestionLevel = Math.min(1.0, currentFlow / capacity);
        }
    }

    public synchronized double getTransmissionEfficiency() {
        if (currentFlow <= 0) return 1.0;
        return (currentFlow - energyLoss) / currentFlow;
    }

    @Override public String toString() { return id + ": " + from.id + " -> " + to.id + " (" + String.format("%.1f", currentFlow) + "/" + capacity + " MW)"; }
    @Override public boolean equals(Object o) { return o instanceof TransmissionLine l && id.equals(l.id); }
    @Override public int hashCode() { return id.hashCode(); }
}
