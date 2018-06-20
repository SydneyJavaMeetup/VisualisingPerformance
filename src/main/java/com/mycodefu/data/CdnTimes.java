package com.mycodefu.data;

import java.util.ArrayList;
import java.util.List;

public class CdnTimes {
    CDN cdn;
    List<CdnBucket> buckets = new ArrayList<>();
    private int totalCount;

    public CdnTimes(CDN cdn) {
        this.cdn = cdn;
    }

    public CdnTimes() {
    }

    public CDN getCdn() {
        return cdn;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public List<CdnBucket> getBuckets() {
        return buckets;
    }

    public void setCdn(CDN cdn) {
        this.cdn = cdn;
    }

    public void addBucket(CdnBucket bucket) {
        buckets.add(bucket);
    }

    public void incrementTotal() {
        totalCount++;
    }
}
