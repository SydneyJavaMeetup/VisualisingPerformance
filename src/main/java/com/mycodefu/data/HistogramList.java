package com.mycodefu.data;

import java.util.ArrayList;
import java.util.List;

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
        for (Histogram cdnTime : histograms) {
            if (cdnTime.group.equals(group)) {
                cdnTime.incrementTotal();
                for (HistogramBucket histogramBucket : cdnTime.buckets) {
                    if (histogramBucket.upperBound == bucketEnd) {
                        histogramBucket.increment();
                        return;
                    }
                }
            }
        }
    }
}
