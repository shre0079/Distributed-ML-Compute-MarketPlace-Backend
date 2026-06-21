package com.dcm.backend.demo.enums;

public enum Priority {
    LOW(0),
    NORMAL(1),
    HIGH(2),
    URGENT(3);

    public final int weight;

    Priority(int weight) {
        this.weight = weight;
    }
}