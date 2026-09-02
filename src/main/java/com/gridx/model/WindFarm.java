package com.gridx.model;

public class WindFarm extends PowerProducer {
    public volatile double windSpeed = 8.0; // m/s
    public final double cutInSpeed = 3.0;
    public final double ratedSpeed = 12.0;
    public final double cutOutSpeed = 25.0;
    public int numTurbines;

    public WindFarm(String id, String name, double x, double y, double maxOutput, double cost) {
        super(id, name, NodeType.WIND_FARM, x, y, maxOutput, cost, 0.35);
        this.numTurbines = (int)(maxOutput / 2.0); // ~2 MW per turbine
    }

    @Override
    public synchronized void update(long currentTick) {
        if (getStatus() != NodeStatus.ACTIVE) {
            currentOutput = 0;
            return;
        }
        if (windSpeed < cutInSpeed || windSpeed > cutOutSpeed) {
            currentOutput = 0;
        } else if (windSpeed < ratedSpeed) {
            // Cube relationship: P = Pmax * ((v - v_cutIn) / (v_rated - v_cutIn))^3
            currentOutput = maxOutput * Math.pow((windSpeed - cutInSpeed) / (ratedSpeed - cutInSpeed), 3);
        } else {
            currentOutput = maxOutput;
        }
        currentOutput *= efficiency;
        addEnergyGenerated(currentOutput);
    }

    public void setWindSpeed(double speed) {
        this.windSpeed = Math.max(0, speed);
    }
}
