package com.gridx.model;

import java.util.concurrent.locks.ReentrantLock;

public class Battery extends EnergyNode {
    public final double capacity; // MWh
    public final double maxChargeRate; // MW
    public final double maxDischargeRate; // MW
    public final double efficiency; // 0-1
    public final double minReserve; // Minimum charge to maintain (MWh)
    private double currentCharge; // MWh
    private final ReentrantLock lock = new ReentrantLock();

    public Battery(String id, String name, double x, double y, double capacity) {
        super(id, name, NodeType.BATTERY, x, y, capacity);
        this.capacity = capacity;
        this.maxChargeRate = capacity * 0.5; // Can fully charge in 2 hours
        this.maxDischargeRate = capacity * 0.8;
        this.efficiency = 0.92;
        this.minReserve = capacity * 0.1; // 10% reserve
        this.currentCharge = capacity * 0.5; // Start half charged
    }

    @Override
    public synchronized void update(long currentTick) {
        // Battery doesn't auto-generate, just maintains state
        updateTimestamp();
    }

    public boolean charge(double amount) {
        lock.lock();
        try {
            if (currentCharge + amount <= capacity) {
                double loss = amount * (1 - efficiency);
                currentCharge += amount - loss;
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    public boolean discharge(double amount) {
        lock.lock();
        try {
            if (currentCharge - amount >= minReserve) {
                double loss = amount * (1 - efficiency);
                currentCharge -= amount;
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    public synchronized double getCurrentCharge() { return currentCharge; }
    public synchronized double getChargePercentage() { return (currentCharge / capacity) * 100; }
    public synchronized double getAvailableDischarge() { return currentCharge - minReserve; }
    public synchronized double getAvailableChargeSpace() { return capacity - currentCharge; }

    public boolean shouldCharge(double currentPrice, double marketPrice) {
        // Charge when prices are low
        return currentPrice < marketPrice * 0.7 && getAvailableChargeSpace() > capacity * 0.1;
    }

    public boolean shouldDischarge(double currentPrice, double marketPrice) {
        // Discharge when prices are high
        return currentPrice > marketPrice * 1.3 && getAvailableDischarge() > 0;
    }
}
