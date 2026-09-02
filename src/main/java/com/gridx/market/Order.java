package com.gridx.market;

import com.gridx.model.*;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Order {
    private static final AtomicInteger ORDER_ID_GENERATOR = new AtomicInteger(1000);
    public final String orderId;
    public final String nodeId;
    public final double quantity; // MW
    public final double maxPrice; // ₹/MWh
    public final LocalDateTime timestamp;
    public volatile OrderStatus status = OrderStatus.PENDING;
    public final double priority;
    public final ConsumerPriority consumerPriority;
    public final long deadlineTick;

    public Order(String nodeId, double quantity, double maxPrice, double priority, ConsumerPriority consumerPriority, long deadlineTick) {
        this.orderId = "ORD-" + ORDER_ID_GENERATOR.incrementAndGet();
        this.nodeId = nodeId;
        this.quantity = quantity;
        this.maxPrice = maxPrice;
        this.priority = priority;
        this.consumerPriority = consumerPriority;
        this.deadlineTick = deadlineTick;
        this.timestamp = LocalDateTime.now();
    }

    @Override public String toString() { return orderId + "[" + nodeId + " x" + String.format("%.1f", quantity) + "MW]"; }
}
