package com.gridx.market;

import java.util.concurrent.atomic.AtomicInteger;

public class Trade {
    private static final AtomicInteger TRADE_ID_GENERATOR = new AtomicInteger(0);
    public final String tradeId;
    public final SellOrder sellOrder;
    public final BuyOrder buyOrder;
    public final double quantity;
    public final double price;
    public final long executionTick;
    public final double energyDelivered;

    public Trade(SellOrder sellOrder, BuyOrder buyOrder, double quantity, double price, long tick, double loss) {
        this.tradeId = "TRD-" + TRADE_ID_GENERATOR.incrementAndGet();
        this.sellOrder = sellOrder;
        this.buyOrder = buyOrder;
        this.quantity = quantity;
        this.price = price;
        this.executionTick = tick;
        this.energyDelivered = quantity - loss;
    }

    public double getTradeValue() { return quantity * price; }
    public double getLoss() { return quantity - energyDelivered; }
    public double getLossPercentage() { return (getLoss() / quantity) * 100; }

    @Override public String toString() { return tradeId + "[" + sellOrder.nodeId + " -> " + buyOrder.nodeId + " " + String.format("%.1f", quantity) + "MW@" + String.format("₹%.2f", price) + "]"; }
}
