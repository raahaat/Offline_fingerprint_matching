package com.offlinematching.receiver.utils;

public class Counter {
    private long count = 0;

    public synchronized void increment() {
        count++;
    }

    public synchronized void decrement() {
        count--;
    }

    public synchronized long value() {
        return count;
    }
}
