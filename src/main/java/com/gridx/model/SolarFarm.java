package com.gridx.model;

public class SolarFarm extends PowerProducer {
    public double solarPanelArea; // m²
    public double panelEfficiency = 0.22; // 22% typical
    public volatile double sunIntensity = 1.0; // 0-1

    public SolarFarm(String id, String name, double x, double y, double maxOutput, double cost) {
        super(id, name, NodeType.SOLAR_FARM, x, y, maxOutput, cost, 0.22);
        this.solarPanelArea = maxOutput * 10000;
    }

    @Override
    public synchronized void update(long currentTick) {
        if (getStatus() != NodeStatus.ACTIVE) {
            currentOutput = 0;
            return;
        }
        // Simulate day/night cycle
        double timeOfDay = (currentTick % 1440) / 60.0;
        sunIntensity = calculateSunIntensity(timeOfDay);
        currentOutput = maxOutput * sunIntensity * efficiency * 0.85;
        addEnergyGenerated(currentOutput);
    }

    private double calculateSunIntensity(double hour) {
        if (hour < 6 || hour > 18) return 0.0;
        if (hour < 8 || hour > 16) return 0.3;
        if (hour < 10 || hour > 14) return 0.7;
        return 1.0;
    }

    public void setCloudCover(double coverage) {
        this.sunIntensity = Math.max(0, 1.0 - coverage);
    }
}
