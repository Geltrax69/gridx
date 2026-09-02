package com.gridx.model;

public class Hospital extends EnergyConsumer {
    public int numBeds;
    public int numICUBeds;
    public boolean backupPower = false;
    public double backupCapacity = 5; // MW

    public Hospital(String id, String name, double x, double y, double demand) {
        super(id, name, NodeType.HOSPITAL, x, y, demand);
        this.priority = ConsumerPriority.CRITICAL;
        this.flexible = false;
        this.minDemand = demand * 0.9; // Hospital needs near full power
        this.maxAcceptablePrice = 50.0; // Will pay anything to stay on
    }

    @Override
    public synchronized void update(long currentTick) {
        if (getStatus() == NodeStatus.ACTIVE) {
            setCurrentLoad(currentDemand);
            addEnergyConsumed(currentDemand);
        } else if (backupPower && currentDemand > minDemand) {
            setCurrentLoad(minDemand);
            addEnergyConsumed(minDemand);
        } else {
            setCurrentLoad(0);
        }
    }
}
