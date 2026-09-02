package com.gridx.model;

public abstract class EnergyConsumer extends EnergyNode {
    public double minDemand; // MW
    public double normalDemand; // MW
    public double peakDemand; // MW
    public double currentDemand; // MW
    public double maxAcceptablePrice; // ₹/MWh
    public ConsumerPriority priority;
    public boolean flexible = false;
    public double flexibleReduction = 0;

    public EnergyConsumer(String id, String name, NodeType type, double x, double y, double demand) {
        super(id, name, type, x, y, demand);
        this.normalDemand = demand;
        this.minDemand = demand * 0.6;
        this.peakDemand = demand * 1.5;
        this.currentDemand = demand;
        this.maxAcceptablePrice = 15.0; // ₹15/MWh default
        this.priority = ConsumerPriority.NORMAL;
    }

    public synchronized double getCurrentDemand() { return currentDemand; }
    public synchronized void setCurrentDemand(double demand) { this.currentDemand = Math.max(minDemand, demand); }

    public synchronized void reduceDemand(double percentage) {
        if (flexible) {
            this.flexibleReduction = percentage;
            this.currentDemand = normalDemand * (1 - percentage);
        }
    }

    public synchronized void restoreDemand() {
        this.flexibleReduction = 0;
        this.currentDemand = normalDemand;
    }

    public double getDemandSatisfaction() {
        if (currentDemand <= 0) return 1.0;
        return Math.min(1.0, getCurrentLoad() / currentDemand);
    }
}
