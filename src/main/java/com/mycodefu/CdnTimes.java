package com.mycodefu;

import java.util.ArrayList;
import java.util.List;

public class CdnTimes {
    CDN cdn;
    List<CdnBucket> buckets = new ArrayList<>();

    public CdnTimes(CDN cdn) {
        this.cdn = cdn;
    }

    public CdnTimes() {
    }

    public CDN getCdn() {
        return cdn;
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

}
