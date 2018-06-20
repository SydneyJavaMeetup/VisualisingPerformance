package com.mycodefu.data;

import java.util.ArrayList;
import java.util.List;

public class CdnSummary {
    List<CdnTimes> cdns = new ArrayList<>();

    public void addCdn(CdnTimes cdnTimes) {
        this.cdns.add(cdnTimes);
    }

    public List<CdnTimes> getCdns() {
        return cdns;
    }

    public void incrementBucket(CDN cdn, int bucketEnd) {
        for (CdnTimes cdnTime : cdns) {
            if (cdnTime.cdn.equals(cdn)) {
                cdnTime.incrementTotal();
                for (CdnBucket cdnBucket : cdnTime.buckets) {
                    if (cdnBucket.bucketUpperMillis == bucketEnd) {
                        cdnBucket.increment();
                        return;
                    }
                }
            }
        }
    }
}
