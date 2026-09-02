package com.gridx.metrics;

import com.gridx.event.*;
import com.gridx.model.*;
import com.gridx.engine.*;
import com.gridx.market.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class MetricsManager {
    private final AtomicLong totalGenerated = new AtomicLong(0);
    private final AtomicLong totalConsumed = new AtomicLong(0);
    private final AtomicLong totalLoss = new AtomicLong(0);
    private final AtomicLong totalTraded = new AtomicLong(0);
    private final AtomicInteger blackoutCount = new AtomicInteger(0);
    private final AtomicLong totalBlackoutDuration = new AtomicLong(0);
    private final AtomicLong totalRevenue = new AtomicLong(0);
    private final AtomicLong totalConsumerCost = new AtomicLong(0);
    private final AtomicInteger loadSheddingEvents = new AtomicInteger(0);
    private final AtomicInteger evsCharged = new AtomicInteger(0);
    private final AtomicInteger evsActive = new AtomicInteger(0);
    private final EventBus eventBus;

    private final BlockingDeque<Double> generationHistory = new LinkedBlockingDeque<>(100);
    private final BlockingDeque<Double> consumptionHistory = new LinkedBlockingDeque<>(100);
    private final BlockingDeque<Double> priceHistory = new LinkedBlockingDeque<>(100);
    private final BlockingDeque<Double> batteryHistory = new LinkedBlockingDeque<>(100);

    public MetricsManager(EventBus eventBus) {
        this.eventBus = eventBus;
        subscribeToEvents();
    }

    private void subscribeToEvents() {
        eventBus.subscribe(EventType.ENERGY_GENERATED, e -> {
            Double amt = e.get("amount");
            if (amt != null) totalGenerated.addAndGet((long) (amt * 1000));
        });
        eventBus.subscribe(EventType.ENERGY_CONSUMED, e -> {
            Double amt = e.get("amount");
            if (amt != null) totalConsumed.addAndGet((long) (amt * 1000));
        });
        eventBus.subscribe(EventType.BLACKOUT, e -> blackoutCount.incrementAndGet());
        eventBus.subscribe(EventType.LOAD_DISCONNECTED, e -> loadSheddingEvents.incrementAndGet());
        eventBus.subscribe(EventType.EV_CHARGING_COMPLETED, e -> evsCharged.incrementAndGet());
        eventBus.subscribe(EventType.TRADE_EXECUTED, e -> {
            Trade trade = e.get("trade");
            if (trade != null) {
                totalTraded.addAndGet((long) (trade.quantity * 1000));
                totalRevenue.addAndGet((long) (trade.price * trade.quantity * 100));
            }
        });
    }

    public void recordGeneration(double mw) {
        addToHistory(generationHistory, mw);
    }

    public void recordConsumption(double mw) {
        addToHistory(consumptionHistory, mw);
    }

    public void recordPrice(double price) {
        addToHistory(priceHistory, price);
    }

    public void recordBatteryLevel(double percentage) {
        addToHistory(batteryHistory, percentage);
    }

    private void addToHistory(BlockingDeque<Double> history, double value) {
        if (history.size() >= 100) history.pollFirst();
        history.offerLast(value);
    }

    public double getTotalGenerated() { return totalGenerated.get() / 1000.0; }
    public double getTotalConsumed() { return totalConsumed.get() / 1000.0; }
    public double getTotalLoss() { return totalLoss.get() / 1000.0; }
    public double getTotalTraded() { return totalTraded.get() / 1000.0; }
    public int getBlackoutCount() { return blackoutCount.get(); }
    public long getTotalBlackoutDuration() { return totalBlackoutDuration.get(); }
    public double getTotalRevenue() { return totalRevenue.get() / 100.0; }
    public double getTotalConsumerCost() { return totalConsumerCost.get() / 100.0; }
    public int getLoadSheddingEvents() { return loadSheddingEvents.get(); }
    public int getEvsCharged() { return evsCharged.get(); }
    public int getEvsActive() { return evsActive.get(); }
    public void setEvsActive(int count) { evsActive.set(count); }

    public List<Double> getGenerationHistory() { return new ArrayList<>(generationHistory); }
    public List<Double> getConsumptionHistory() { return new ArrayList<>(consumptionHistory); }
    public List<Double> getPriceHistory() { return new ArrayList<>(priceHistory); }
    public List<Double> getBatteryHistory() { return new ArrayList<>(batteryHistory); }
}
