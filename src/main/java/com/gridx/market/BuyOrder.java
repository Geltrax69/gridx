package com.gridx.market;

import com.gridx.model.*;

public class BuyOrder extends Order {
    public final EnergyConsumer consumer;

    public BuyOrder(EnergyConsumer consumer, double quantity, double maxPrice, long deadlineTick) {
        super(consumer.id, quantity, maxPrice, consumer.priority.level, consumer.priority, deadlineTick);
        this.consumer = consumer;
    }
}
