package com.gridx.engine;

import com.gridx.event.*;
import java.util.concurrent.atomic.*;

public class PricingEngine {
    private final AtomicReference<Double> currentPrice = new AtomicReference<>(5.0);
    private final AtomicLong totalRevenue = new AtomicLong(0);
    private final AtomicLong totalConsumerCost = new AtomicLong(0);
    private double basePrice = 5.0;
    private double demandWeight = 0.3;
    private double supplyWeight = 0.3;
    private double batteryWeight = 0.2;
    private double timeWeight = 0.2;

    public double calculatePrice(double generation, double consumption, double batteryLevel, int hourOfDay) {
        double demandRatio = consumption / Math.max(1, generation);
        double supplyRatio = generation / Math.max(1, consumption);
        double batteryRatio = batteryLevel / 100.0;
        double timeMultiplier = getTimeMultiplier(hourOfDay);

        double price = basePrice;
        price += demandRatio * demandWeight * basePrice;
        price += (1 - supplyRatio) * supplyWeight * basePrice;
        price += (1 - batteryRatio) * batteryWeight * basePrice;
        price *= timeMultiplier;

        // Cap prices
        price = Math.max(1.0, Math.min(50.0, price));
        currentPrice.set(price);
        return price;
    }

    private double getTimeMultiplier(int hour) {
        if (hour >= 17 && hour < 21) return 1.3; // Peak
        if (hour >= 6 && hour < 9) return 1.2;   // Morning peak
        if (hour >= 22 || hour < 6) return 0.7;  // Off-peak
        return 1.0;
    }

    public double getCurrentPrice() { return currentPrice.get(); }
    public void setBasePrice(double price) { this.basePrice = price; }
    public void addRevenue(double amount) { totalRevenue.addAndGet((long) amount); }
    public void addConsumerCost(double amount) { totalConsumerCost.addAndGet((long) amount); }
    public double getTotalRevenue() { return totalRevenue.get() / 100.0; }
    public double getTotalConsumerCost() { return totalConsumerCost.get() / 100.0; }
}
