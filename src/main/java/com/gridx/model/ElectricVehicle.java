package com.gridx.model;

import java.util.concurrent.atomic.AtomicReference;

public class ElectricVehicle {
    public final String id;
    public final double batteryCapacity; // kWh
    public final double maxChargingRate; // kW
    public final long deadline; // When must be fully charged (tick)
    public final AtomicReference<Double> currentCharge = new AtomicReference<>(0.0);
    public volatile long arrivalTick;
    public volatile String stationId;
    public volatile boolean charging = false;
    public volatile double pricePaid = 0;

    public ElectricVehicle(String id, double capacity, long arrivalTick, long deadline) {
        this.id = id;
        this.batteryCapacity = capacity;
        this.maxChargingRate = Math.min(50, capacity * 0.3); // Typical EV charger
        this.arrivalTick = arrivalTick;
        this.deadline = deadline;
        this.currentCharge.set(capacity * 0.2); // Start at 20%
    }

    public double getChargePercentage() {
        return (currentCharge.get() / batteryCapacity) * 100;
    }

    public double getEnergyNeeded() {
        return batteryCapacity - currentCharge.get();
    }

    public long getTicksUntilDeadline() {
        return Math.max(0, deadline - arrivalTick);
    }
}
