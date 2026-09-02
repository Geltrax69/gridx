package com.gridx.engine;

import com.gridx.event.*;
import com.gridx.model.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class EnergyBalancer {
    private final List<EnergyNode> allNodes;
    private final List<Battery> batteries;
    private final EventBus eventBus;
    private final AtomicLong totalGeneration = new AtomicLong(0);
    private final AtomicLong totalConsumption = new AtomicLong(0);
    private final AtomicLong totalLoss = new AtomicLong(0);
    private final AtomicLong gridDeficit = new AtomicLong(0);
    private final AtomicLong gridSurplus = new AtomicLong(0);
    private final ReentrantLock balanceLock = new ReentrantLock();

    public EnergyBalancer(List<EnergyNode> allNodes, List<Battery> batteries, EventBus eventBus) {
        this.allNodes = allNodes;
        this.batteries = batteries;
        this.eventBus = eventBus;
    }

    public BalanceResult balance(long currentTick) {
        balanceLock.lock();
        try {
            double generation = 0, consumption = 0, loss = 0;
            generation = allNodes.stream()
                .filter(n -> n instanceof PowerProducer)
                .mapToDouble(n -> ((PowerProducer) n).currentOutput)
                .sum();
            consumption = allNodes.stream()
                .filter(n -> n instanceof EnergyConsumer)
                .mapToDouble(n -> ((EnergyConsumer) n).currentDemand)
                .sum();
            loss = allNodes.stream()
                .flatMap(n -> n.getConnections().stream())
                .mapToDouble(TransmissionLine::getEnergyLoss)
                .sum();

            totalGeneration.set((long)(generation * 1000));
            totalConsumption.set((long)(consumption * 1000));
            totalLoss.set((long)(loss * 1000));

            double balance = generation - consumption;

            BalanceResult result = new BalanceResult(generation, consumption, loss, balance);

            if (balance < 0) {
                // Deficit: use batteries, start generators, reduce demand
                double deficit = -balance;
                result.setDeficit(deficit);
                handleDeficit(deficit, result);
            } else if (balance > 0) {
                // Surplus: charge batteries
                double surplus = balance;
                result.setSurplus(surplus);
                handleSurplus(surplus, result);
            } else {
                result.setBalanced(true);
            }

            eventBus.publish(new Event(EventType.GRID_BALANCED).with("result", result));
            return result;
        } finally {
            balanceLock.unlock();
        }
    }

    private void handleDeficit(double deficit, BalanceResult result) {
        // 1. Discharge batteries
        double discharged = dischargeBatteries(deficit);
        deficit -= discharged;
        result.setBatteryDischarged(discharged);

        // 2. Start additional generators
        if (deficit > 0) {
            double generated = startBackupGenerators(deficit);
            deficit -= generated;
            result.setAdditionalGeneration(generated);
        }

        // 3. Reduce flexible demand
        if (deficit > 0) {
            double reduced = reduceFlexibleDemand(deficit);
            deficit -= reduced;
            result.setDemandReduced(reduced);
        }

        // 4. If still deficit, shed low-priority loads
        if (deficit > 0.1) {
            double shed = shedLoads(deficit);
            result.setLoadShed(shed);
            eventBus.publish(new Event(EventType.ENERGY_SHORTAGE)
                .with("deficit", deficit)
                .with("shed", shed));
        }

        if (deficit > 5.0) {
            eventBus.publish(new Event(EventType.BLACKOUT_WARNING).with("deficit", deficit));
        }
    }

    private void handleSurplus(double surplus, BalanceResult result) {
        // Charge batteries with surplus energy
        double charged = chargeBatteries(surplus);
        surplus -= charged;
        result.setBatteryCharged(charged);

        // Reduce generator output
        if (surplus > 0) {
            double reduced = reduceGeneratorOutput(surplus);
            result.setGenerationReduced(reduced);
        }

        eventBus.publish(new Event(EventType.ENERGY_SURPLUS).with("surplus", surplus));
    }

    private double dischargeBatteries(double amount) {
        double discharged = 0;
        // Sort by charge level descending
        batteries.sort((a, b) -> Double.compare(b.getCurrentCharge(), a.getCurrentCharge()));
        for (Battery battery : batteries) {
            if (amount <= 0) break;
            if (battery.getStatus() == NodeStatus.ACTIVE) {
                double available = battery.getAvailableDischarge();
                double toDischarge = Math.min(available, amount);
                if (battery.discharge(toDischarge)) {
                    discharged += toDischarge;
                    amount -= toDischarge;
                    eventBus.publish(new Event(EventType.BATTERY_DISCHARGED)
                        .with("battery", battery)
                        .with("amount", toDischarge));
                }
            }
        }
        return discharged;
    }

    private double chargeBatteries(double amount) {
        double charged = 0;
        // Sort by available space descending
        batteries.sort((a, b) -> Double.compare(b.getAvailableChargeSpace(), a.getAvailableChargeSpace()));
        for (Battery battery : batteries) {
            if (amount <= 0) break;
            if (battery.getStatus() == NodeStatus.ACTIVE) {
                double space = battery.getAvailableChargeSpace();
                double toCharge = Math.min(space, amount);
                if (battery.charge(toCharge)) {
                    charged += toCharge;
                    amount -= toCharge;
                    eventBus.publish(new Event(EventType.BATTERY_CHARGED)
                        .with("battery", battery)
                        .with("amount", toCharge));
                }
            }
        }
        return charged;
    }

    private double startBackupGenerators(double amount) {
        double generated = 0;
        for (EnergyNode node : allNodes) {
            if (node instanceof Generator generator) {
                if (amount <= 0) break;
                if (!generator.running && generator.getStatus() == NodeStatus.ACTIVE) {
                    generator.start();
                    generated += generator.maxOutput;
                    amount -= generator.maxOutput;
                }
            }
        }
        return generated;
    }

    private double reduceGeneratorOutput(double amount) {
        double reduced = 0;
        // Reduce expensive generators first
        List<PowerProducer> producers = allNodes.stream()
            .filter(n -> n instanceof PowerProducer)
            .map(n -> (PowerProducer) n)
            .sorted((a, b) -> Double.compare(b.productionCost, a.productionCost))
            .toList();

        for (PowerProducer producer : producers) {
            if (amount <= 0) break;
            if (producer.currentOutput > 0) {
                double toReduce = Math.min(producer.currentOutput, amount);
                producer.setCurrentOutput(producer.currentOutput - toReduce);
                reduced += toReduce;
                amount -= toReduce;
            }
        }
        return reduced;
    }

    private double reduceFlexibleDemand(double amount) {
        double reduced = 0;
        List<EnergyConsumer> consumers = allNodes.stream()
            .filter(n -> n instanceof EnergyConsumer)
            .map(n -> (EnergyConsumer) n)
            .sorted(Comparator.comparingInt((EnergyConsumer c) -> c.priority.level).reversed())
            .toList();

        for (EnergyConsumer consumer : consumers) {
            if (amount <= 0) break;
            if (consumer.flexible && consumer.getStatus() == NodeStatus.ACTIVE) {
                double reduction = consumer.currentDemand * 0.3; // Reduce by 30%
                consumer.reduceDemand(0.3);
                reduced += reduction;
                amount -= reduction;
            }
        }
        return reduced;
    }

    private double shedLoads(double amount) {
        double shed = 0;
        // Shed lowest priority first
        List<EnergyConsumer> consumers = allNodes.stream()
            .filter(n -> n instanceof EnergyConsumer)
            .map(n -> (EnergyConsumer) n)
            .sorted(Comparator.comparingInt((EnergyConsumer c) -> c.priority.level).reversed())
            .toList();

        for (EnergyConsumer consumer : consumers) {
            if (amount <= 0) break;
            if (consumer.getStatus() == NodeStatus.ACTIVE && consumer.priority.level > ConsumerPriority.EMERGENCY.level) {
                double toShed = consumer.currentDemand;
                consumer.setStatus(NodeStatus.OFFLINE);
                consumer.setCurrentLoad(0);
                shed += toShed;
                amount -= toShed;
                eventBus.publish(new Event(EventType.LOAD_DISCONNECTED)
                    .with("consumer", consumer)
                    .with("amount", toShed));
            }
        }
        return shed;
    }

    public double getTotalGeneration() { return totalGeneration.get() / 1000.0; }
    public double getTotalConsumption() { return totalConsumption.get() / 1000.0; }
    public double getTotalLoss() { return totalLoss.get() / 1000.0; }
    public double getGridBalance() { return getTotalGeneration() - getTotalConsumption(); }
    public double getBatteryAverageCharge() {
        if (batteries.isEmpty()) return 0;
        return batteries.stream().mapToDouble(Battery::getChargePercentage).average().orElse(0);
    }

    public static class BalanceResult {
        public final double generation;
        public final double consumption;
        public final double loss;
        public final double balance;
        public double deficit = 0;
        public double surplus = 0;
        public double batteryDischarged = 0;
        public double batteryCharged = 0;
        public double additionalGeneration = 0;
        public double generationReduced = 0;
        public double demandReduced = 0;
        public double loadShed = 0;
        public boolean balanced = false;

        public BalanceResult(double gen, double cons, double loss, double bal) {
            this.generation = gen;
            this.consumption = cons;
            this.loss = loss;
            this.balance = bal;
        }

        public void setDeficit(double d) { this.deficit = d; }
        public void setSurplus(double s) { this.surplus = s; }
        public void setBatteryDischarged(double d) { this.batteryDischarged = d; }
        public void setBatteryCharged(double c) { this.batteryCharged = c; }
        public void setAdditionalGeneration(double g) { this.additionalGeneration = g; }
        public void setGenerationReduced(double g) { this.generationReduced = g; }
        public void setDemandReduced(double d) { this.demandReduced = d; }
        public void setLoadShed(double s) { this.loadShed = s; }
        public void setBalanced(boolean b) { this.balanced = b; }

        @Override public String toString() {
            return String.format("Balance{gen=%.1f, cons=%.1f, loss=%.1f, balance=%+.1f}",
                generation, consumption, loss, balance);
        }
    }
}
