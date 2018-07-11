package com.mycodefu.visualisingperformance.data;

import java.util.ArrayList;
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

    public static HistogramList of(int bucketSize, int numberOfBuckets) {
        return of(bucketSize, numberOfBuckets, Collections.singletonList(""));
    }

    public static List<Integer> buckets(int bucketSize, int numberOfBuckets) {
        return IntStream
                .rangeClosed(0, numberOfBuckets)
                .map(bucketNo -> bucketNo * bucketSize)
                .boxed()
                .collect(Collectors.toList());
    }

    public static HistogramList of(int bucketSize, int numberOfBuckets, List<String> groups) {
        HistogramList histogramList = new HistogramList();
        histogramList.addHistograms(
                groups.stream().map(group -> Histogram.of(bucketSize, numberOfBuckets, group)).collect(Collectors.toList())
        );
        return histogramList;
    }

}
