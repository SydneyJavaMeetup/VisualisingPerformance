package com.mycodefu.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class HistogramList {
    List<Histogram> histograms = new ArrayList<>();

    public List<Histogram> getHistograms() {
        return histograms;
    }

    public void addHistogram(Histogram histogram) {
        this.histograms.add(histogram);
    }

    public void addHistograms(List<Histogram> histograms) {
        this.histograms.addAll(histograms);
    }

    public void incrementBucket(String group, int bucketEnd) {
        for (Histogram histogram : histograms) {
            if (histogram.group.equals(group)) {
                histogram.incrementTotal();
                for (HistogramBucket histogramBucket : histogram.buckets) {
                    if (histogramBucket.upperBound == bucketEnd) {
                        histogramBucket.increment();
                        return;
                    }
                }
            }
        }
    }

    public static HistogramList of(int bucketSize, int numberOfBuckets) {
        return of(bucketSize, numberOfBuckets, Collections.singletonList(""));
    }

    public static HistogramList of(int bucketSize, int numberOfBuckets, List<String> groups) {
        HistogramList histogramList = new HistogramList();
        histogramList.addHistograms(
                groups.stream().map(group -> {
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
                }).collect(Collectors.toList())
        );
        return histogramList;
    }

}
