package com.gridx.model;

public class Factory extends EnergyConsumer {
    public String industryType;
    public int numMachines;
    public boolean hasFlexibleProduction = true;

    public Factory(String id, String name, String industry, double x, double y, double demand) {
        super(id, name, NodeType.FACTORY, x, y, demand);
        this.industryType = industry;
        this.priority = ConsumerPriority.INDUSTRIAL;
        this.flexible = true;
        this.maxAcceptablePrice = 12.0;
        this.numMachines = (int)(demand / 0.5); // 500 kW per machine
    }

    @Override
    public synchronized void update(long currentTick) {
        if (getStatus() != NodeStatus.ACTIVE) {
            setCurrentLoad(0);
            return;
        }

        double timeOfDay = (currentTick % 1440) / 60.0;
        double demandMultiplier = getFactoryDemandMultiplier(timeOfDay);

        currentDemand = normalDemand * demandMultiplier * (1 - flexibleReduction);
        setCurrentLoad(currentDemand);
        addEnergyConsumed(currentDemand);
    }

    private double getFactoryDemandMultiplier(double hour) {
        // Factories typically work during the day
        if (hour >= 8 && hour < 18) return 1.2;
        if (hour >= 6 && hour < 22) return 0.9;
        return 0.5; // Night shift reduced
    }
}
