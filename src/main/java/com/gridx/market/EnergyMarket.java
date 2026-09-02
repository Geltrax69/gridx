package com.gridx.market;

import com.gridx.event.*;
import com.gridx.model.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class EnergyMarket {
    private final PriorityBlockingQueue<BuyOrder> buyOrderBook = new PriorityBlockingQueue<>(100,
        Comparator.comparingDouble((BuyOrder o) -> -o.maxPrice) // Highest price first
    );
    private final PriorityBlockingQueue<SellOrder> sellOrderBook = new PriorityBlockingQueue<>(100,
        Comparator.comparingDouble((SellOrder o) -> o.minPrice) // Lowest price first
    );
    private final List<Trade> tradeHistory = new CopyOnWriteArrayList<>();
    private final EventBus eventBus;
    private final ReentrantReadWriteLock marketLock = new ReentrantReadWriteLock();
    private volatile double lastClearingPrice = 5.0;
    private final AtomicLong totalEnergyTraded = new AtomicLong(0);
    private final AtomicLong totalRevenue = new AtomicLong(0);
    private final AtomicInteger rejectedOrders = new AtomicInteger(0);

    public EnergyMarket(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void submitSellOrder(SellOrder order) {
        marketLock.writeLock().lock();
        try {
            sellOrderBook.offer(order);
            eventBus.publish(new Event(EventType.MARKET_ORDER_CREATED).with("order", order));
        } finally {
            marketLock.writeLock().unlock();
        }
    }

    public void submitBuyOrder(BuyOrder order) {
        marketLock.writeLock().lock();
        try {
            buyOrderBook.offer(order);
            eventBus.publish(new Event(EventType.MARKET_ORDER_CREATED).with("order", order));
        } finally {
            marketLock.writeLock().unlock();
        }
    }

    public List<Trade> matchOrders(long currentTick) {
        List<Trade> trades = new ArrayList<>();
        marketLock.writeLock().lock();
        try {
            // Greedy matching: lowest sell price to highest buy price
            while (!sellOrderBook.isEmpty() && !buyOrderBook.isEmpty()) {
                SellOrder sell = sellOrderBook.peek();
                BuyOrder buy = buyOrderBook.peek();

                if (sell == null || buy == null) break;

                // Check if buyer's max price >= seller's min price
                if (buy.maxPrice < sell.minPrice) {
                    // No more matches possible
                    break;
                }

                // Calculate clearing price
                double clearingPrice = (sell.minPrice + buy.maxPrice) / 2.0;

                // Determine trade quantity
                double tradeQuantity = Math.min(sell.quantity, buy.quantity);

                // Calculate transmission loss
                double loss = calculateTransmissionLoss(sell, buy);
                double delivered = Math.max(0, tradeQuantity - loss);

                // Create trade
                Trade trade = new Trade(sell, buy, tradeQuantity, clearingPrice, currentTick, loss);
                trades.add(trade);
                tradeHistory.add(trade);

                // Update metrics
                totalEnergyTraded.addAndGet((long)(delivered * 1000));
                totalRevenue.addAndGet((long)(tradeQuantity * clearingPrice));

                // Update orders
                updateOrderQuantities(sell, buy, tradeQuantity);

                // Update buyer's load
                if (buy.consumer != null) {
                    buy.consumer.setCurrentLoad(buy.consumer.getCurrentLoad() + delivered);
                }

                // Publish trade event
                eventBus.publish(new Event(EventType.TRADE_EXECUTED).with("trade", trade));

                lastClearingPrice = clearingPrice;
            }
        } finally {
            marketLock.writeLock().unlock();
        }
        return trades;
    }

    private double calculateTransmissionLoss(SellOrder sell, BuyOrder buy) {
        // Loss based on distance between nodes (simplified)
        // In real system, would use the actual transmission line
        return sell.quantity * 0.03; // 3% average loss
    }

    private void updateOrderQuantities(SellOrder sell, BuyOrder buy, double tradedQty) {
        // Update or remove sell order
        if (sell.quantity - tradedQty < 0.01) {
            sellOrderBook.poll();
        } else {
            // Recreate with reduced quantity (since SellOrder is immutable)
            sellOrderBook.remove(sell);
            SellOrder reduced = new SellOrder(sell.producer, sell.quantity - tradedQty, sell.minPrice, sell.transmissionLoss);
            sellOrderBook.offer(reduced);
        }

        // Update or remove buy order
        if (buy.quantity - tradedQty < 0.01) {
            buyOrderBook.poll();
        } else {
            buyOrderBook.remove(buy);
            BuyOrder reduced = new BuyOrder(buy.consumer, buy.quantity - tradedQty, buy.maxPrice, buy.deadlineTick);
            buyOrderBook.offer(reduced);
        }
    }

    public List<Trade> matchOrdersMultiFactor(long currentTick) {
        List<Trade> trades = new ArrayList<>();
        marketLock.writeLock().lock();
        try {
            // Multi-factor matching: consider price, distance, reliability, priority
            List<BuyOrder> buys = new ArrayList<>(buyOrderBook);
            List<SellOrder> sells = new ArrayList<>(sellOrderBook);

            // Sort by composite score
            buys.sort((a, b) -> Double.compare(calculateBuyScore(b), calculateBuyScore(a)));
            sells.sort((a, b) -> Double.compare(calculateSellScore(a), calculateSellScore(b)));

            for (BuyOrder buy : buys) {
                if (buy.status == OrderStatus.MATCHED) continue;
                for (SellOrder sell : sells) {
                    if (sell.status == OrderStatus.MATCHED) continue;
                    if (buy.maxPrice < sell.minPrice) continue;

                    double clearingPrice = (sell.minPrice + buy.maxPrice) / 2.0;
                    double tradeQuantity = Math.min(sell.quantity, buy.quantity);
                    double loss = calculateTransmissionLoss(sell, buy);
                    double delivered = Math.max(0, tradeQuantity - loss);

                    Trade trade = new Trade(sell, buy, tradeQuantity, clearingPrice, currentTick, loss);
                    trades.add(trade);
                    tradeHistory.add(trade);
                    totalEnergyTraded.addAndGet((long)(delivered * 1000));
                    totalRevenue.addAndGet((long)(tradeQuantity * clearingPrice));

                    buy.status = OrderStatus.MATCHED;
                    sell.status = OrderStatus.MATCHED;
                    lastClearingPrice = clearingPrice;

                    eventBus.publish(new Event(EventType.TRADE_EXECUTED).with("trade", trade));
                    break;
                }
            }

            // Remove matched orders
            buyOrderBook.removeIf(o -> o.status == OrderStatus.MATCHED);
            sellOrderBook.removeIf(o -> o.status == OrderStatus.MATCHED);
        } finally {
            marketLock.writeLock().unlock();
        }
        return trades;
    }

    private double calculateBuyScore(BuyOrder order) {
        // Higher price + higher priority = better
        return order.maxPrice * 0.5 + (7 - order.consumerPriority.level) * 10;
    }

    private double calculateSellScore(SellOrder order) {
        // Lower price + higher reliability = better
        return -order.minPrice + order.producer.reliability * 50;
    }

    public double getLastClearingPrice() { return lastClearingPrice; }
    public long getTotalEnergyTraded() { return totalEnergyTraded.get(); }
    public long getTotalRevenue() { return totalRevenue.get(); }
    public int getRejectedOrders() { return rejectedOrders.get(); }
    public int getBuyOrderCount() { return buyOrderBook.size(); }
    public int getSellOrderCount() { return sellOrderBook.size(); }
    public List<Trade> getTradeHistory() { return new ArrayList<>(tradeHistory); }
}
