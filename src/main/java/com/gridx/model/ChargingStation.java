package com.gridx.model;

import java.util.*;
import java.util.concurrent.*;

public class ChargingStation extends EnergyNode {
    public final int numChargers;
    public final double powerPerCharger; // kW
    public final Queue<ElectricVehicle> waitingQueue = new ConcurrentLinkedQueue<>();
    public final List<ElectricVehicle> activeVehicles = new CopyOnWriteArrayList<>();
    private final double totalCapacity;

    public ChargingStation(String id, String name, double x, double y, int numChargers, double powerPerCharger) {
        super(id, name, NodeType.EV_STATION, x, y, numChargers * powerPerCharger);
        this.numChargers = numChargers;
        this.powerPerCharger = powerPerCharger;
        this.totalCapacity = numChargers * powerPerCharger;
    }

    public synchronized boolean addVehicle(ElectricVehicle ev) {
        ev.stationId = id;
        if (activeVehicles.size() < numChargers) {
            activeVehicles.add(ev);
            ev.charging = true;
            return true;
        } else {
            waitingQueue.offer(ev);
            return false;
        }
    }

    public synchronized void removeVehicle(ElectricVehicle ev) {
        activeVehicles.remove(ev);
        ev.charging = false;
        ElectricVehicle next = waitingQueue.poll();
        if (next != null) {
            activeVehicles.add(next);
            next.charging = true;
        }
    }

    @Override
    public synchronized void update(long currentTick) {
        // Update charging progress
        Iterator<ElectricVehicle> it = activeVehicles.iterator();
        while (it.hasNext()) {
            ElectricVehicle ev = it.next();
            if (ev.getChargePercentage() >= 95 || currentTick >= ev.deadline) {
                it.remove();
                ev.charging = false;
            } else {
                double chargeAmount = Math.min(ev.maxChargingRate, ev.getEnergyNeeded());
                ev.currentCharge.updateAndGet(c -> c + chargeAmount);
            }
        }
        setCurrentLoad(getCurrentLoad());
    }

    public synchronized int getQueueSize() { return waitingQueue.size(); }
    public synchronized int getActiveCount() { return activeVehicles.size(); }
    public synchronized double getUtilization() { return (double) activeVehicles.size() / numChargers; }
}
