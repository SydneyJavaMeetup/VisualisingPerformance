package com.mycodefu.visualisingperformance.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Histogram {
    String group;
    List<HistogramBucket> buckets = new ArrayList<>();
    int totalCount;

    public Histogram(String group) {
        this.group = group;
    }

    public Histogram() {
    }

    public String getGroup() {
        return group;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public List<HistogramBucket> getBuckets() {
        return buckets;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public void addBucket(HistogramBucket bucket) {
        buckets.add(bucket);
    }
    public void addBuckets(Collection<HistogramBucket> buckets) {
        this.buckets.addAll(buckets);
    }

    public void incrementTotal() {
        totalCount++;
    }
}
