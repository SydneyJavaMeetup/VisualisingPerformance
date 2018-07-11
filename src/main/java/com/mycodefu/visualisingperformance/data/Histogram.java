package com.mycodefu.visualisingperformance.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

    public void incrementTotal(Integer count) {
        totalCount += count;
    }


    public void setBucketCount(int upperBound, int count) {
        for (HistogramBucket bucket : buckets) {
            if (bucket.getUpperBound() == upperBound) {
                bucket.setCount(count);
                break;
            }
        }
    }

    public static Histogram of(int bucketSize, int numberOfBuckets, String group) {
        Histogram histogram = new Histogram();
        histogram.setGroup(group);
        histogram.addBuckets(
                IntStream
                        .rangeClosed(1, numberOfBuckets)
                        .map(bucketNo -> bucketNo * bucketSize)
                        .mapToObj(bucketEnd ->
                                new HistogramBucket(
                                        String.format("%d-%d", bucketEnd - bucketSize, bucketEnd),
                                        bucketEnd,
                                        0
                                ))
                        .collect(Collectors.toCollection(ArrayList::new))
        );
        return histogram;
    }
}
