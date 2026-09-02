package com.gridx.ui;

import com.gridx.model.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

public class GridPanel extends JPanel {
    private final List<EnergyNode> nodes;
    private final List<TransmissionLine> lines;
    private EnergyNode hoveredNode = null;
    private TransmissionLine hoveredLine = null;
    private long currentTick = 0;

    public GridPanel(List<EnergyNode> nodes, List<TransmissionLine> lines) {
        this.nodes = nodes;
        this.lines = lines;
        setBackground(new Color(20, 30, 45));
        setPreferredSize(new Dimension(800, 700));
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                handleHover(e.getX(), e.getY());
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                handleClick(e.getX(), e.getY());
            }
        });
    }

    public void setCurrentTick(long tick) {
        this.currentTick = tick;
    }

    private void handleHover(int x, int y) {
        hoveredNode = null;
        for (EnergyNode node : nodes) {
            if (Math.hypot(x - node.x, y - node.y) < 15) {
                hoveredNode = node;
                break;
            }
        }
        hoveredLine = null;
        for (TransmissionLine line : lines) {
            if (distanceToLine(x, y, line) < 5) {
                hoveredLine = line;
                break;
            }
        }
        repaint();
    }

    private void handleClick(int x, int y) {
        if (hoveredNode != null) {
            showNodeInfo(hoveredNode);
        } else if (hoveredLine != null) {
            showLineInfo(hoveredLine);
        }
    }

    private double distanceToLine(int px, int py, TransmissionLine line) {
        double x1 = line.from.x, y1 = line.from.y;
        double x2 = line.to.x, y2 = line.to.y;
        double dx = x2 - x1, dy = y2 - y1;
        double length2 = dx * dx + dy * dy;
        if (length2 == 0) return Math.hypot(px - x1, py - y1);
        double t = Math.max(0, Math.min(1, ((px - x1) * dx + (py - y1) * dy) / length2));
        double projectionX = x1 + t * dx;
        double projectionY = y1 + t * dy;
        return Math.hypot(px - projectionX, py - projectionY);
    }

    private void showNodeInfo(EnergyNode node) {
        String info = String.format("ID: %s\nName: %s\nType: %s\nStatus: %s\nLoad: %.1f MW",
            node.id, node.name, node.type, node.getStatus(), node.getCurrentLoad());
        if (node instanceof PowerProducer p) {
            info += String.format("\nOutput: %.1f MW\nCost: ₹%.2f/MWh", p.currentOutput, p.productionCost);
        } else if (node instanceof Battery b) {
            info += String.format("\nCharge: %.1f%%\nCapacity: %.1f MWh", b.getChargePercentage(), b.capacity);
        } else if (node instanceof EnergyConsumer c) {
            info += String.format("\nDemand: %.1f MW\nPriority: %s", c.currentDemand, c.priority);
        }
        JOptionPane.showMessageDialog(this, info, node.name, JOptionPane.INFORMATION_MESSAGE);
    }

    private void showLineInfo(TransmissionLine line) {
        String info = String.format("ID: %s\nFrom: %s\nTo: %s\nFlow: %.1f MW\nCapacity: %.1f MW\nLoss: %.2f MW",
            line.id, line.from.name, line.to.name, line.getCurrentFlow(), line.capacity, line.getEnergyLoss());
        JOptionPane.showMessageDialog(this, info, "Transmission Line", JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw transmission lines
        for (TransmissionLine line : lines) {
            Color color = getLineColor(line);
            g2d.setColor(color);
            g2d.setStroke(new BasicStroke(Math.max(1, (float) (line.getCurrentFlow() / 50))));
            g2d.drawLine((int)line.from.x, (int)line.from.y, (int)line.to.x, (int)line.to.y);

            // Animate energy flow
            if (line.getCurrentFlow() > 0 && line.isActive()) {
                animateFlow(g2d, line);
            }
        }

        // Draw nodes
        for (EnergyNode node : nodes) {
            drawNode(g2d, node);
        }
    }

    private void animateFlow(Graphics2D g2d, TransmissionLine line) {
        double progress = (currentTick % 10) / 10.0;
        double x = line.from.x + (line.to.x - line.from.x) * progress;
        double y = line.from.y + (line.to.y - line.from.y) * progress;
        g2d.setColor(new Color(255, 255, 100, 200));
        g2d.fillOval((int) x - 3, (int) y - 3, 6, 6);
    }

    private Color getLineColor(TransmissionLine line) {
        if (line.getStatus() == LineStatus.FAILED) return Color.RED;
        double congestion = line.getCongestionPercentage();
        if (congestion > 80) return new Color(255, 50, 50);
        if (congestion > 50) return new Color(255, 200, 50);
        return new Color(100, 200, 255);
    }

    private void drawNode(Graphics2D g2d, EnergyNode node) {
        int x = (int) node.x;
        int y = (int) node.y;
        int size = node.type == NodeType.SUBSTATION ? 25 : 18;

        Color color = getNodeColor(node);
        if (node.getStatus() != NodeStatus.ACTIVE) {
            g2d.setColor(Color.DARK_GRAY);
        } else {
            g2d.setColor(color);
        }
        g2d.fillOval(x - size/2, y - size/2, size, size);
        g2d.setColor(Color.WHITE);
        g2d.drawOval(x - size/2, y - size/2, size, size);

        if (node == hoveredNode) {
            g2d.setColor(Color.YELLOW);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval(x - size/2 - 3, y - size/2 - 3, size + 6, size + 6);
            g2d.setStroke(new BasicStroke(1));
        }

        // Label
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 8));
        g2d.drawString(node.name, x + size/2 + 2, y + 3);
    }

    private Color getNodeColor(EnergyNode node) {
        if (node instanceof SolarFarm) return new Color(255, 200, 50);
        if (node instanceof WindFarm) return new Color(100, 200, 255);
        if (node instanceof Generator) return new Color(200, 100, 200);
        if (node instanceof Battery) return new Color(100, 255, 100);
        if (node instanceof Hospital) return new Color(255, 100, 100);
        if (node instanceof Factory) return new Color(150, 100, 50);
        if (node.type == NodeType.OFFICE) return new Color(150, 150, 200);
        if (node instanceof Home) return new Color(200, 200, 100);
        if (node instanceof ChargingStation) return new Color(100, 255, 200);
        if (node.type == NodeType.SUBSTATION) return new Color(200, 200, 200);
        return Color.WHITE;
    }
}
