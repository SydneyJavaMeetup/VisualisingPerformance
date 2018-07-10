package com.mycodefu.visualisingperformance.dataaccess;

import com.mongodb.async.client.MongoClient;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.BsonField;
import com.mongodb.client.model.BucketOptions;
import com.mycodefu.visualisingperformance.data.Histogram;
import com.mycodefu.visualisingperformance.data.HistogramBucket;
import com.mycodefu.visualisingperformance.data.HistogramList;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.mongodb.client.model.Aggregates.bucket;
import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Filters.*;
import static org.apache.logging.log4j.message.MapMessage.MapFormat.JSON;

public class PerfStatsDataAccess {
    private MongoClient mongoClient;

    public PerfStatsDataAccess(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }

    public CompletableFuture<HistogramList> histogramStatsSince(String timestampString, String toTimestampString, String countryCode, String queryCapString, String bucketSizeString, String statName) {

        long timestamp = Long.parseLong(timestampString);
        long toTimestamp =StringUtils.isBlank(toTimestampString) ? Instant.now().toEpochMilli() : Long.parseLong(toTimestampString);

        int queryCap;
        if (StringUtils.isNotBlank(queryCapString)) {
            queryCap = Integer.parseInt(queryCapString);
        } else {
            queryCap = 10_000;
        }
        int bucketSize;
        if (StringUtils.isNotBlank(bucketSizeString)) {
            bucketSize = Integer.parseInt(bucketSizeString);
        } else {
            bucketSize = 500;
        }
        int numberOfBuckets = queryCap / bucketSize;

        if (StringUtils.isBlank(statName)) {
            statName = "img-large";
        }

        List<String> cdns = new ArrayList<>();
        cdns.add("CloudFront");
        boolean includeAliCloud = "CN".equalsIgnoreCase(countryCode);
        if (includeAliCloud) {
            cdns.add("AliCloud");
        }

        //Create empty buckets
        HistogramList histogramList = HistogramList.of(bucketSize, numberOfBuckets, cdns);

        //Fill up the buckets!
        List<Bson> query = createQuery(timestamp, toTimestamp, countryCode, 60 * 24, bucketSize, numberOfBuckets);
        return queryDatabase(query, statName, histogramList, queryCap, bucketSize, includeAliCloud);
    }

    private int roundToBucketEnd(double value, int bucketSize) {
        return (int) (Math.ceil(value / bucketSize) * bucketSize);
    }

    private List<Bson> createQuery(long timestamp, long toTimestamp, String countryCode, int defaultMinutes, int bucketSize, int numberOfBuckets) {
        Bson query;

        Instant from;
        if (timestamp <= 0) {
            from = Instant.now().minus(defaultMinutes, ChronoUnit.MINUTES);
        } else {
            from = Instant.ofEpochMilli(timestamp);
        }

        query = gte("timestamp", from);
        if (toTimestamp > 0) {
            query = and(query, lt("timestamp", Instant.ofEpochMilli(toTimestamp).toString()));
        }
        if (StringUtils.isNotBlank(countryCode)) {
            query = and(query, eq("countryCode", countryCode));
        }

        BucketOptions options = new BucketOptions();
        options.defaultBucket("default");
        options.output(new BsonField("count", eq("$sum", 1)));
        return Arrays.asList(
                match(query),
                bucket("$timestamp",
                        HistogramList.buckets(bucketSize, numberOfBuckets),
                        options)
        );
    }

    private CompletableFuture<HistogramList> queryDatabase(List<Bson> query, String statName, HistogramList histogramList, Integer queryCap, Integer bucketSize, boolean includeAliCloud) {
        CompletableFuture<HistogramList> result = new CompletableFuture<>();

        mongoClient
                .getDatabase("SydneyJavaMeetup")
                .getCollection("PerfStats")
                .aggregate(query)
                .forEach(stat -> {
                    System.out.println(stat);
//                    Integer cloudFront = stat.getInteger("CloudFront");
//                    if (cloudFront != null && cloudFront < queryCap && cloudFront > 0) {
//                        histogramList.incrementBucket("CloudFront", roundToBucketEnd(cloudFront, bucketSize));
//                    }
//                    if (includeAliCloud) {
//                        Integer aliCloud = stat.getInteger("AliCloud");
//                        if (aliCloud!=null && aliCloud < queryCap && aliCloud > 0) {
//                            histogramList.incrementBucket("AliCloud", roundToBucketEnd(aliCloud, bucketSize));
//                        }
//                    }
        }, (aVoid, throwable) -> {
            if (throwable != null) {
                result.completeExceptionally(throwable);
            } else {
                result.complete(histogramList);
            }
        });

        return result;
    }
}
