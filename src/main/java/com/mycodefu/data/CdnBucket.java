package com.mycodefu.data;

public class CdnBucket {
    String bucketName;
    Integer bucketUpperMillis;
    Integer count;

    public CdnBucket(String bucketName, Integer bucketUpperMillis, Integer count) {
        this.bucketName = bucketName;
        this.bucketUpperMillis = bucketUpperMillis;
        this.count = count;
    }

    public CdnBucket() {
    }

    public String getBucketName() {
        return bucketName;
    }

    public Integer getBucketUpperMillis() {
        return bucketUpperMillis;
    }

    public Integer getCount() {
        return count;
    }

    public void increment() {
        count++;
    }
}
