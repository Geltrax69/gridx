package com.gridx.model;

public abstract class PowerProducer extends EnergyNode {
    public volatile double currentOutput; // MW
    public volatile double maxOutput; // MW
    public volatile double productionCost; // ₹/MWh
    public volatile double efficiency; // 0-1
    public volatile double reliability; // 0-1 (historical uptime)

    public PowerProducer(String id, String name, NodeType type, double x, double y, double maxOutput, double cost, double efficiency) {
        super(id, name, type, x, y, maxOutput);
        this.maxOutput = maxOutput;
        this.productionCost = cost;
        this.efficiency = efficiency;
        this.reliability = 0.95;
        this.currentOutput = 0;
    }

    public synchronized double getCurrentOutput() { return currentOutput; }
    public synchronized void setCurrentOutput(double output) {
        this.currentOutput = Math.max(0, Math.min(output, maxOutput));
    }

    public synchronized double getReliability() { return reliability; }
    public synchronized void setReliability(double r) { this.reliability = Math.max(0, Math.min(1, r)); }

    public synchronized double getProductionCost() { return productionCost; }
    public synchronized void setProductionCost(double cost) { this.productionCost = cost; }

    public double getEstimatedRevenue(double hoursOfOperation) {
        return currentOutput * productionCost * hoursOfOperation;
    }
}
