package com.gridx.simulation;

import com.gridx.model.*;
import java.util.*;

public class GridBuilder {
    public static List<EnergyNode> buildGrid() {
        List<EnergyNode> nodes = new ArrayList<>();

        // Create substations
        Substation main = new Substation("SUB-MAIN", "Main Substation", 400, 300, 1000);
        Substation north = new Substation("SUB-NORTH", "North Substation", 400, 100, 500);
        Substation south = new Substation("SUB-SOUTH", "South Substation", 400, 500, 500);
        nodes.add(main); nodes.add(north); nodes.add(south);

        // Create power producers
        SolarFarm solar1 = new SolarFarm("SOLAR-1", "North Solar Farm", 150, 100, 200, 3.0);
        SolarFarm solar2 = new SolarFarm("SOLAR-2", "East Solar Farm", 650, 250, 150, 3.5);
        SolarFarm solar3 = new SolarFarm("SOLAR-3", "South Solar Farm", 200, 500, 100, 4.0);
        nodes.add(solar1); nodes.add(solar2); nodes.add(solar3);

        WindFarm wind1 = new WindFarm("WIND-1", "Coastal Wind", 100, 300, 180, 4.5);
        WindFarm wind2 = new WindFarm("WIND-2", "Highland Wind", 650, 400, 120, 5.0);
        nodes.add(wind1); nodes.add(wind2);

        Generator gen1 = new Generator("GEN-1", "Central Gas Plant", 300, 300, 300, 8.0);
        Generator gen2 = new Generator("GEN-2", "Industrial Generator", 500, 350, 200, 9.0);
        nodes.add(gen1); nodes.add(gen2);

        // Create batteries
        Battery bat1 = new Battery("BAT-1", "Central Battery", 400, 200, 100);
        Battery bat2 = new Battery("BAT-2", "North Battery", 400, 120, 50);
        Battery bat3 = new Battery("BAT-3", "South Battery", 350, 450, 75);
        nodes.add(bat1); nodes.add(bat2); nodes.add(bat3);

        // Create hospitals
        Hospital hosp1 = new Hospital("HOSP-1", "Central Hospital", 500, 200, 50);
        Hospital hosp2 = new Hospital("HOSP-2", "Regional Hospital", 300, 450, 40);
        nodes.add(hosp1); nodes.add(hosp2);

        // Create factories
        Factory fact1 = new Factory("FACT-1", "Steel Works", "Steel", 150, 350, 80);
        Factory fact2 = new Factory("FACT-2", "Auto Plant", "Automotive", 600, 450, 120);
        Factory fact3 = new Factory("FACT-3", "Chemical Plant", "Chemicals", 250, 200, 60);
        nodes.add(fact1); nodes.add(fact2); nodes.add(fact3);

        // Create offices
        Office off1 = new Office("OFF-1", "Tech Park", 500, 280, 30);
        Office off2 = new Office("OFF-2", "Business Center", 450, 350, 25);
        nodes.add(off1); nodes.add(off2);

        // Create homes
        for (int i = 0; i < 50; i++) {
            double x = 100 + Math.random() * 550;
            double y = 150 + Math.random() * 350;
            Home home = new Home("HOME-" + (i + 1), "Home " + (i + 1), x, y);
            nodes.add(home);
        }

        // Create EV stations
        ChargingStation ev1 = new ChargingStation("EV-1", "Downtown EV Hub", 400, 280, 10, 50);
        ChargingStation ev2 = new ChargingStation("EV-2", "Mall Charging", 500, 320, 8, 50);
        nodes.add(ev1); nodes.add(ev2);

        return nodes;
    }

    public static List<TransmissionLine> buildConnections(List<EnergyNode> nodes) {
        List<TransmissionLine> lines = new ArrayList<>();

        // Create connections
        connect(nodes, "SUB-MAIN", "SUB-NORTH", lines, 800, 0.02, 10);
        connect(nodes, "SUB-MAIN", "SUB-SOUTH", lines, 800, 0.02, 10);
        connect(nodes, "SUB-NORTH", "SOLAR-1", lines, 200, 0.01, 5);
        connect(nodes, "SUB-MAIN", "SOLAR-2", lines, 250, 0.01, 5);
        connect(nodes, "SUB-SOUTH", "SOLAR-3", lines, 200, 0.01, 5);
        connect(nodes, "SUB-MAIN", "WIND-1", lines, 300, 0.015, 8);
        connect(nodes, "SUB-SOUTH", "WIND-2", lines, 250, 0.015, 8);
        connect(nodes, "SUB-MAIN", "GEN-1", lines, 200, 0.01, 5);
        connect(nodes, "SUB-MAIN", "GEN-2", lines, 200, 0.01, 5);
        connect(nodes, "SUB-MAIN", "BAT-1", lines, 150, 0.01, 5);
        connect(nodes, "SUB-NORTH", "BAT-2", lines, 150, 0.01, 5);
        connect(nodes, "SUB-SOUTH", "BAT-3", lines, 150, 0.01, 5);
        connect(nodes, "SUB-MAIN", "HOSP-1", lines, 100, 0.005, 3);
        connect(nodes, "SUB-SOUTH", "HOSP-2", lines, 100, 0.005, 3);
        connect(nodes, "SUB-MAIN", "FACT-1", lines, 200, 0.01, 5);
        connect(nodes, "SUB-SOUTH", "FACT-2", lines, 200, 0.01, 5);
        connect(nodes, "SUB-NORTH", "FACT-3", lines, 150, 0.01, 5);
        connect(nodes, "SUB-MAIN", "OFF-1", lines, 100, 0.005, 3);
        connect(nodes, "SUB-MAIN", "OFF-2", lines, 100, 0.005, 3);
        connect(nodes, "SUB-MAIN", "EV-1", lines, 100, 0.005, 3);
        connect(nodes, "SUB-MAIN", "EV-2", lines, 100, 0.005, 3);

        // Connect homes
        int homeCount = 0;
        for (EnergyNode node : nodes) {
            if (node instanceof Home) {
                String subId = homeCount % 2 == 0 ? "SUB-NORTH" : "SUB-SOUTH";
                connect(nodes, subId, node.id, lines, 50, 0.005, 2);
                homeCount++;
            }
        }

        return lines;
    }

    private static void connect(List<EnergyNode> nodes, String fromId, String toId, List<TransmissionLine> lines, double capacity, double resistance, double length) {
        EnergyNode from = findNode(nodes, fromId);
        EnergyNode to = findNode(nodes, toId);
        if (from != null && to != null) {
            TransmissionLine line = new TransmissionLine(fromId + "-" + toId, from, to, capacity, resistance, length);
            lines.add(line);
            from.addConnection(line);
            to.addConnection(line);
        }
    }

    public static EnergyNode findNode(List<EnergyNode> nodes, String id) {
        return nodes.stream().filter(n -> n.id.equals(id)).findFirst().orElse(null);
    }
}

class Substation extends EnergyNode {
    public Substation(String id, String name, double x, double y, double capacity) {
        super(id, name, NodeType.SUBSTATION, x, y, capacity);
    }
    @Override public void update(long tick) { updateTimestamp(); }
}

class Office extends EnergyConsumer {
    public Office(String id, String name, double x, double y, double demand) {
        super(id, name, NodeType.OFFICE, x, y, demand);
        this.priority = ConsumerPriority.COMMERCIAL;
        this.minDemand = demand * 0.7;
    }
    @Override
    public synchronized void update(long currentTick) {
        if (getStatus() != NodeStatus.ACTIVE) { setCurrentLoad(0); return; }
        double timeOfDay = (currentTick % 1440) / 60.0;
        double multiplier = (timeOfDay >= 9 && timeOfDay < 17) ? 1.0 : 0.3;
        currentDemand = normalDemand * multiplier;
        setCurrentLoad(currentDemand);
        addEnergyConsumed(currentDemand);
    }
}
