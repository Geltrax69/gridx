package com.gridx.market;

import com.gridx.model.*;

public class SellOrder extends Order {
    public final PowerProducer producer;
    public final double minPrice;
    public final double transmissionLoss;

    public SellOrder(PowerProducer producer, double quantity, double minPrice, double transmissionLoss) {
        super(producer.id, quantity, minPrice, producer.reliability, ConsumerPriority.INDUSTRIAL, Long.MAX_VALUE);
        this.producer = producer;
        this.minPrice = minPrice;
        this.transmissionLoss = transmissionLoss;
    }

    public double getEffectivePrice(double lossFactor) {
        return minPrice * (1 + lossFactor);
    }
}
