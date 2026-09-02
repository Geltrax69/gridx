package com.gridx.engine;

import com.gridx.event.*;
import com.gridx.model.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class FailureInjector {
    private final List<EnergyNode> nodes;
    private final List<TransmissionLine> lines;
    private final EventBus eventBus;
    private final ScheduledExecutorService scheduler;
    private final Random random = new Random();
    private volatile boolean running = false;
    private final AtomicInteger failureCount = new AtomicInteger(0);

    public FailureInjector(List<EnergyNode> nodes, List<TransmissionLine> lines, EventBus eventBus) {
        this.nodes = nodes;
        this.lines = lines;
        this.eventBus = eventBus;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "FailureInjector");
            t.setDaemon(true);
            return t;
        });
    }

    public void start() {
        running = true;
        scheduler.scheduleAtFixedRate(this::injectRandomFailure, 10, 20, TimeUnit.SECONDS);
    }

    public void stop() { running = false; scheduler.shutdown(); }

    public void injectPowerPlantFailure() {
        List<PowerProducer> producers = nodes.stream()
            .filter(n -> n instanceof PowerProducer)
            .map(n -> (PowerProducer) n)
            .toList();
        if (producers.isEmpty()) return;
        PowerProducer victim = producers.get(random.nextInt(producers.size()));
        victim.setStatus(NodeStatus.FAILED);
        victim.setCurrentOutput(0);
        failureCount.incrementAndGet();
        eventBus.publish(new Event(EventType.POWER_PLANT_FAILED).with("producer", victim));
    }

    public void injectTransmissionFailure() {
        if (lines.isEmpty()) return;
        TransmissionLine line = lines.get(random.nextInt(lines.size()));
        line.setStatus(LineStatus.FAILED);
        failureCount.incrementAndGet();
        eventBus.publish(new Event(EventType.GRID_CONGESTION).with("line", line));
    }

    public void injectBatteryFailure() {
        if (nodes.stream().noneMatch(n -> n instanceof Battery)) return;
        Battery battery = (Battery) nodes.stream().filter(n -> n instanceof Battery).findAny().orElse(null);
        if (battery != null) {
            battery.setStatus(NodeStatus.FAILED);
            failureCount.incrementAndGet();
            eventBus.publish(new Event(EventType.POWER_PLANT_FAILED).with("battery", battery));
        }
    }

    public void injectSuddenDemandSpike() {
        // Increase all consumer demands by 50% temporarily
        for (EnergyNode node : nodes) {
            if (node instanceof EnergyConsumer consumer) {
                consumer.setCurrentDemand(consumer.currentDemand * 1.5);
            }
        }
        failureCount.incrementAndGet();
        eventBus.publish(new Event(EventType.ENERGY_SHORTAGE));
    }

    public void injectWindDrop() {
        for (EnergyNode node : nodes) {
            if (node instanceof WindFarm wind) {
                wind.setWindSpeed(1.0); // Below cut-in
            }
        }
        failureCount.incrementAndGet();
        eventBus.publish(new Event(EventType.ENERGY_SHORTAGE));
    }

    public void injectCloudCover() {
        for (EnergyNode node : nodes) {
            if (node instanceof SolarFarm solar) {
                solar.setCloudCover(0.8);
            }
        }
        failureCount.incrementAndGet();
        eventBus.publish(new Event(EventType.ENERGY_SHORTAGE));
    }

    public void injectHeatWave() {
        // Increase home demand
        for (EnergyNode node : nodes) {
            if (node instanceof Home home) {
                home.normalDemand = 8.0; // Double from 5
            } else if (node.type == NodeType.OFFICE) {
                // Office demand increases during heat wave
            }
        }
        failureCount.incrementAndGet();
        eventBus.publish(new Event(EventType.ENERGY_SHORTAGE));
    }

    public void injectBlackout() {
        // Force a system-wide blackout
        for (EnergyNode node : nodes) {
            if (node instanceof EnergyConsumer consumer && consumer.priority.level > ConsumerPriority.EMERGENCY.level) {
                consumer.setStatus(NodeStatus.OFFLINE);
            }
        }
        failureCount.incrementAndGet();
        eventBus.publish(new Event(EventType.BLACKOUT));
    }

    public void injectGridAttack() {
        // Random sabotage
        for (int i = 0; i < 3; i++) {
            if (random.nextBoolean()) injectPowerPlantFailure();
            else injectTransmissionFailure();
        }
        failureCount.incrementAndGet();
    }

    public void injectRecovery() {
        for (EnergyNode node : nodes) {
            if (node.getStatus() == NodeStatus.OFFLINE) {
                node.setStatus(NodeStatus.ACTIVE);
            }
        }
        for (TransmissionLine line : lines) {
            if (line.getStatus() == LineStatus.FAILED) {
                line.setStatus(LineStatus.ACTIVE);
            }
        }
        eventBus.publish(new Event(EventType.RECOVERY_INITIATED));
    }

    private void injectRandomFailure() {
        if (!running) return;
        int choice = random.nextInt(7);
        switch (choice) {
            case 0 -> injectPowerPlantFailure();
            case 1 -> injectTransmissionFailure();
            case 2 -> injectWindDrop();
            case 3 -> injectCloudCover();
            case 4 -> injectSuddenDemandSpike();
            case 5 -> injectBatteryFailure();
            case 6 -> injectGridAttack();
        }
    }

    public int getFailureCount() { return failureCount.get(); }
}
