package com.gridx.ui;

import com.gridx.event.*;
import com.gridx.engine.*;
import com.gridx.simulation.*;
import com.gridx.metrics.*;
import com.gridx.model.*;
import com.gridx.market.*;
import java.util.List;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DashboardFrame extends JFrame {
    private final List<EnergyNode> nodes;
    private final List<TransmissionLine> lines;
    private final EventBus eventBus;
    private final SimulationEngine simulationEngine;
    private final MetricsManager metricsManager;
    private final FailureInjector failureInjector;
    private final GridPanel gridPanel;
    private final JTextArea eventLog;
    private JLabel[] statusLabels;
    private DefaultListModel<String> marketModel;

    public DashboardFrame() {
        super("GridX - Intelligent Energy Marketplace & Smart Grid Simulator");

        this.nodes = GridBuilder.buildGrid();
        this.lines = GridBuilder.buildConnections(nodes);
        this.eventBus = new EventBus();
        this.simulationEngine = new SimulationEngine(nodes, lines, eventBus);
        this.metricsManager = new MetricsManager(eventBus);
        this.failureInjector = new FailureInjector(nodes, lines, eventBus);

        eventBus.start();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setSize(1400, 900);

        // Grid Panel
        gridPanel = new GridPanel(nodes, lines);
        add(gridPanel, BorderLayout.CENTER);

        // Control Panel
        JPanel controlPanel = createControlPanel();
        add(controlPanel, BorderLayout.WEST);

        // Status Panel
        JPanel statusPanel = createStatusPanel();
        add(statusPanel, BorderLayout.NORTH);

        // Market Panel
        JPanel marketPanel = createMarketPanel();
        add(marketPanel, BorderLayout.SOUTH);

        // Event Log
        eventLog = new JTextArea(8, 50);
        eventLog.setEditable(false);
        eventLog.setFont(new Font("Monospaced", Font.PLAIN, 10));
        JScrollPane logScroll = new JScrollPane(eventLog);
        logScroll.setBorder(BorderFactory.createTitledBorder("Event Log"));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, marketPanel, logScroll);
        split.setDividerLocation(200);
        add(split, BorderLayout.EAST);

        // Subscribe to events
        subscribeToEvents();

        // Update timer
        javax.swing.Timer timer = new javax.swing.Timer(500, e -> updateDisplay());
        timer.start();

        setLocationRelativeTo(null);
        setVisible(true);

        logEvent("GridX initialized. Press Start to begin simulation.");
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setPreferredSize(new Dimension(250, 0));
        panel.setBorder(BorderFactory.createTitledBorder("Controls"));

        // Simulation controls
        JPanel simControls = new JPanel(new GridLayout(2, 2, 5, 5));
        JButton startBtn = new JButton("▶ Start");
        JButton pauseBtn = new JButton("⏸ Pause");
        JButton resumeBtn = new JButton("▶ Resume");
        JButton resetBtn = new JButton("↻ Reset");

        startBtn.addActionListener(e -> {
            simulationEngine.start();
            failureInjector.start();
            startBtn.setEnabled(false);
            pauseBtn.setEnabled(true);
        });
        pauseBtn.addActionListener(e -> {
            simulationEngine.pause();
            pauseBtn.setEnabled(false);
            resumeBtn.setEnabled(true);
        });
        resumeBtn.addActionListener(e -> {
            simulationEngine.resume();
            pauseBtn.setEnabled(true);
            resumeBtn.setEnabled(false);
        });
        pauseBtn.setEnabled(false);
        resumeBtn.setEnabled(false);

        simControls.add(startBtn);
        simControls.add(pauseBtn);
        simControls.add(resumeBtn);
        simControls.add(resetBtn);

        // Speed control
        JPanel speedPanel = new JPanel(new FlowLayout());
        speedPanel.add(new JLabel("Speed:"));
        JComboBox<String> speedBox = new JComboBox<>(new String[]{"1x", "2x", "5x", "10x"});
        speedBox.addActionListener(e -> {
            int speed = switch (speedBox.getSelectedIndex()) {
                case 0 -> 1; case 1 -> 2; case 2 -> 5; case 3 -> 10;
                default -> 1;
            };
            simulationEngine.setSpeed(speed);
        });
        speedPanel.add(speedBox);

        // Failure injection
        JPanel failurePanel = new JPanel(new GridLayout(4, 2, 3, 3));
        failurePanel.setBorder(BorderFactory.createTitledBorder("Failure Injection"));
        addFailureButton(failurePanel, "Power Plant", e -> failureInjector.injectPowerPlantFailure());
        addFailureButton(failurePanel, "Transmission", e -> failureInjector.injectTransmissionFailure());
        addFailureButton(failurePanel, "Battery", e -> failureInjector.injectBatteryFailure());
        addFailureButton(failurePanel, "Demand Spike", e -> failureInjector.injectSuddenDemandSpike());
        addFailureButton(failurePanel, "Wind Drop", e -> failureInjector.injectWindDrop());
        addFailureButton(failurePanel, "Cloud Cover", e -> failureInjector.injectCloudCover());
        addFailureButton(failurePanel, "Heat Wave", e -> failureInjector.injectHeatWave());
        addFailureButton(failurePanel, "Blackout", e -> failureInjector.injectBlackout());

        panel.add(simControls, BorderLayout.NORTH);
        panel.add(speedPanel, BorderLayout.CENTER);
        panel.add(failurePanel, BorderLayout.SOUTH);

        return panel;
    }

    private void addFailureButton(JPanel panel, String label, ActionListener action) {
        JButton btn = new JButton(label);
        btn.addActionListener(e -> {
            action.actionPerformed(e);
            logEvent("FAILURE INJECTED: " + label);
        });
        panel.add(btn);
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 8, 10, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Grid Status"));
        panel.setPreferredSize(new Dimension(100, 60));

        statusLabels = new JLabel[8];
        String[] labels = {"Generation", "Consumption", "Battery", "Price", "Grid Load", "Blackouts", "Traded", "Events"};
        for (int i = 0; i < 8; i++) {
            statusLabels[i] = new JLabel(labels[i] + ": 0");
            statusLabels[i].setFont(new Font("Arial", Font.BOLD, 11));
            panel.add(statusLabels[i]);
        }

        return panel;
    }

    private JPanel createMarketPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Energy Market"));

        marketModel = new DefaultListModel<>();
        JList<String> marketList = new JList<>(marketModel);
        panel.add(new JScrollPane(marketList), BorderLayout.CENTER);

        return panel;
    }

    private void subscribeToEvents() {
        eventBus.subscribe(EventType.ENERGY_SHORTAGE, e -> logEvent("⚠ ENERGY SHORTAGE: " + e.get("deficit") + " MW deficit"));
        eventBus.subscribe(EventType.ENERGY_SURPLUS, e -> logEvent("✓ Energy surplus: " + e.get("surplus") + " MW"));
        eventBus.subscribe(EventType.BLACKOUT, e -> logEvent("🔴 BLACKOUT!"));
        eventBus.subscribe(EventType.BLACKOUT_WARNING, e -> logEvent("⚠ BLACKOUT WARNING!"));
        eventBus.subscribe(EventType.POWER_PLANT_FAILED, e -> {
            Object producer = e.get("producer");
            logEvent("❌ POWER PLANT FAILED: " + (producer != null ? producer : "Unknown"));
        });
        eventBus.subscribe(EventType.TRADE_EXECUTED, e -> {
            Trade trade = e.get("trade");
            if (trade != null) marketModel.addElement(trade.toString());
            if (marketModel.getSize() > 100) marketModel.remove(0);
        });
        eventBus.subscribe(EventType.BATTERY_DISCHARGED, e -> logEvent("🔋 Battery discharge: " + e.get("amount") + " MW"));
        eventBus.subscribe(EventType.BATTERY_CHARGED, e -> logEvent("⚡ Battery charge: " + e.get("amount") + " MW"));
        eventBus.subscribe(EventType.GRID_CONGESTION, e -> logEvent("⚠ Transmission congestion"));
        eventBus.subscribe(EventType.LOAD_DISCONNECTED, e -> logEvent("⚡ Load disconnected: " + e.get("consumer")));
        eventBus.subscribe(EventType.PRICE_CHANGED, e -> {
            Double price = e.get("price");
            if (price != null) logEvent("₹ Price changed: ₹" + String.format("%.2f", price) + "/MWh");
        });
    }

    private void updateDisplay() {
        long tick = simulationEngine.getSimulationTick();
        gridPanel.setCurrentTick(tick);
        gridPanel.repaint();

        EnergyBalancer.BalanceResult balance = simulationEngine.getLatestBalance();
        double batteryLevel = simulationEngine.getNodes().stream()
            .filter(n -> n instanceof Battery)
            .mapToDouble(n -> ((Battery) n).getChargePercentage())
            .average().orElse(0);

        statusLabels[0].setText("Gen: " + String.format("%.0f MW", balance.generation));
        statusLabels[1].setText("Cons: " + String.format("%.0f MW", balance.consumption));
        statusLabels[2].setText("Bat: " + String.format("%.0f%%", batteryLevel));
        statusLabels[3].setText("Price: ₹" + String.format("%.2f", simulationEngine.getPricingEngine().getCurrentPrice()));
        statusLabels[4].setText("Load: " + String.format("%.0f%%", (balance.consumption / Math.max(1, balance.generation)) * 100));
        statusLabels[5].setText("Blackouts: " + metricsManager.getBlackoutCount());
        statusLabels[6].setText("Traded: " + String.format("%.0f MWh", metricsManager.getTotalTraded()));
        statusLabels[7].setText("Tick: " + tick);
    }

    private void logEvent(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        SwingUtilities.invokeLater(() -> {
            eventLog.append("[" + timestamp + "] " + message + "\n");
            eventLog.setCaretPosition(eventLog.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DashboardFrame());
    }
}
