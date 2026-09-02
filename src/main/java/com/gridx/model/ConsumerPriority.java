package com.gridx.model;

public enum ConsumerPriority {
    CRITICAL(1), EMERGENCY(2), NORMAL(3), RESIDENTIAL(4), COMMERCIAL(5), INDUSTRIAL(6), EV_CHARGING(7);
    public final int level;
    ConsumerPriority(int level) { this.level = level; }
}
