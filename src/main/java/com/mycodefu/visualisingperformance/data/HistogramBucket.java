package com.mycodefu.visualisingperformance.data;

public class HistogramBucket {
    String name;
    Integer upperBound;
    Integer count;

    public HistogramBucket(String name, Integer upperBound, Integer count) {
        this.name = name;
        this.upperBound = upperBound;
        this.count = count;
    }

    public HistogramBucket() {
    }

    public String getName() {
        return name;
    }

    public Integer getUpperBound() {
        return upperBound;
    }

    public Integer getCount() {
        return count;
    }

    public void increment() {
        count++;
    }
}
