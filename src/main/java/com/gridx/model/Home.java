package com.gridx.model;

import java.util.concurrent.ThreadLocalRandom;

public class Home extends EnergyConsumer {
    public int numResidents;
    public boolean hasSolar = false;
    public double solarGeneration = 0;
    public boolean hasEV = false;
    public double evBattery = 0;

    public Home(String id, String name, double x, double y) {
        super(id, name, NodeType.HOME, x, y, 5.0); // 5 kW typical home
        this.priority = ConsumerPriority.RESIDENTIAL;
        this.numResidents = ThreadLocalRandom.current().nextInt(1, 6);
    }

    @Override
    public synchronized void update(long currentTick) {
        if (getStatus() != NodeStatus.ACTIVE) {
            setCurrentLoad(0);
            return;
        }

        double timeOfDay = (currentTick % 1440) / 60.0;
        double demandMultiplier = getDemandMultiplier(timeOfDay);

        // Peak demand in morning and evening
        currentDemand = normalDemand * demandMultiplier;

        // Add EV charging if applicable
        if (hasEV && evBattery < 100) {
            currentDemand += 3.0; // 3 kW charging
        }

        setCurrentLoad(currentDemand);
        addEnergyConsumed(currentDemand);
    }

    private double getDemandMultiplier(double hour) {
        if (hour >= 6 && hour < 9) return 1.3; // Morning peak
        if (hour >= 17 && hour < 21) return 1.5; // Evening peak
        if (hour >= 22 || hour < 6) return 0.4; // Night
        return 0.8; // Daytime
    }
}
