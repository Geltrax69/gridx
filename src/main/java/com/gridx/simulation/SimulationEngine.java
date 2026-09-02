package com.gridx.simulation;

import com.gridx.event.*;
import com.gridx.engine.*;
import com.gridx.market.*;
import com.gridx.model.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class SimulationEngine {
    private final List<EnergyNode> nodes;
    private final List<TransmissionLine> lines;
    private final List<Battery> batteries;
    private final EventBus eventBus;
    private final EnergyBalancer energyBalancer;
    private final EnergyMarket market;
    private final PricingEngine pricingEngine;
    private final ScheduledExecutorService scheduler;
    private volatile boolean running = false;
    private volatile boolean paused = false;
    private final AtomicLong simulationTick = new AtomicLong(0);
    private volatile int simulationSpeed = 1;
    private final long startTime;

    public SimulationEngine(List<EnergyNode> nodes, List<TransmissionLine> lines, EventBus eventBus) {
        this.nodes = nodes;
        this.lines = lines;
        this.eventBus = eventBus;
        this.startTime = System.currentTimeMillis();

        this.batteries = nodes.stream()
            .filter(n -> n instanceof Battery)
            .map(n -> (Battery) n)
            .collect(java.util.stream.Collectors.toList());

        this.energyBalancer = new EnergyBalancer(nodes, batteries, eventBus);
        this.market = new EnergyMarket(eventBus);
        this.pricingEngine = new PricingEngine();

        this.scheduler = Executors.newScheduledThreadPool(4, r -> {
            Thread t = new Thread(r, "Simulation");
            t.setDaemon(true);
            return t;
        });

        subscribeToEvents();
    }

    private void subscribeToEvents() {
        eventBus.subscribe(EventType.SIMULATION_STARTED, e -> startSimulation());
        eventBus.subscribe(EventType.SIMULATION_PAUSED, e -> paused = true);
        eventBus.subscribe(EventType.SIMULATION_RESUMED, e -> paused = false);
        eventBus.subscribe(EventType.SIMULATION_STOPPED, e -> stopSimulation());
    }

    public void start() {
        running = true;
        paused = false;
        eventBus.publish(new Event(EventType.SIMULATION_STARTED));
        startSimulationLoop();
    }

    public void stop() {
        running = false;
        eventBus.publish(new Event(EventType.SIMULATION_STOPPED));
    }

    public void pause() { paused = true; eventBus.publish(new Event(EventType.SIMULATION_PAUSED)); }
    public void resume() { paused = false; eventBus.publish(new Event(EventType.SIMULATION_RESUMED)); }
    public void setSpeed(int speed) { this.simulationSpeed = speed; }

    private void startSimulationLoop() {
        scheduler.scheduleAtFixedRate(this::tick, 0, 1000 / simulationSpeed, TimeUnit.MILLISECONDS);
    }

    public void startSimulation() { running = true; paused = false; }
    public void stopSimulation() { running = false; }

    private void tick() {
        if (!running || paused) return;

        long currentTick = simulationTick.incrementAndGet();

        // Update all nodes
        for (EnergyNode node : nodes) {
            node.update(currentTick);
        }

        // Balance the grid
        EnergyBalancer.BalanceResult balance = energyBalancer.balance(currentTick);

        // Update market pricing
        int hour = (int) ((currentTick % 1440) / 60);
        double price = pricingEngine.calculatePrice(
            balance.generation, balance.consumption,
            energyBalancer.getBatteryAverageCharge(), hour);
        eventBus.publish(new Event(EventType.PRICE_CHANGED).with("price", price));

        // Check for blackouts
        if (balance.loadShed > balance.generation * 0.5) {
            eventBus.publish(new Event(EventType.BLACKOUT));
        }

        // Generate market orders periodically
        if (currentTick % 10 == 0) {
            generateMarketOrders();
            market.matchOrders(currentTick);
        }

        eventBus.publish(new Event(EventType.MARKET_CLEARED).with("price", price));
    }

    private void generateMarketOrders() {
        // Producers sell excess energy
        for (EnergyNode node : nodes) {
            if (node instanceof PowerProducer producer) {
                if (producer.currentOutput > producer.maxOutput * 0.3) {
                    double excess = producer.currentOutput - producer.maxOutput * 0.3;
                    market.submitSellOrder(new SellOrder(producer, excess, producer.productionCost, 0.03));
                }
            }
        }
    }

    public EnergyBalancer.BalanceResult getLatestBalance() {
        return energyBalancer.balance(simulationTick.get());
    }

    public long getSimulationTick() { return simulationTick.get(); }
    public boolean isRunning() { return running; }
    public boolean isPaused() { return paused; }
    public int getSpeed() { return simulationSpeed; }
    public EnergyMarket getMarket() { return market; }
    public PricingEngine getPricingEngine() { return pricingEngine; }
    public List<EnergyNode> getNodes() { return nodes; }
    public List<TransmissionLine> getLines() { return lines; }
}
