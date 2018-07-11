package com.mycodefu.visualisingperformance.dataaccess;

import com.mongodb.async.client.MongoClient;
import com.mongodb.client.model.BsonField;
import com.mongodb.client.model.BucketOptions;
import com.mycodefu.visualisingperformance.data.Histogram;
import com.mycodefu.visualisingperformance.data.HistogramBucket;
import com.mycodefu.visualisingperformance.data.HistogramList;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.conversions.Bson;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.mongodb.client.model.Aggregates.bucket;
import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Filters.*;

public class PerfStatsDataAccess {
    static final Logger log = LogManager.getLogger(PerfStatsDataAccess.class);

    private MongoClient mongoClient;

    public PerfStatsDataAccess(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }

    public HistogramList histogramStatsSince(String timestampString, String toTimestampString, String countryCode, String queryCapString, String bucketSizeString, String statNameParameter) {
        final long timestamp = Long.parseLong(timestampString);
        final long toTimestamp =StringUtils.isBlank(toTimestampString) ? Instant.now().toEpochMilli() : Long.parseLong(toTimestampString);

        final int queryCap;
        if (StringUtils.isNotBlank(queryCapString)) {
            queryCap = Integer.parseInt(queryCapString);
        } else {
            queryCap = 10_000;
        }
        final int bucketSize;
        if (StringUtils.isNotBlank(bucketSizeString)) {
            bucketSize = Integer.parseInt(bucketSizeString);
        } else {
            bucketSize = 500;
        }
        final int numberOfBuckets = queryCap / bucketSize;

        final String statName;
        if (StringUtils.isBlank(statNameParameter)) {
            statName = "img-large";
        } else {
            statName = statNameParameter;
        }

        List<String> cdns = new ArrayList<>();
        cdns.add("CloudFront");
        boolean includeAliCloud = "CN".equalsIgnoreCase(countryCode);
        if (includeAliCloud) {
            cdns.add("AliCloud");
        }

        Instant start = Instant.now();
        HistogramList histogramList = new HistogramList();
        cdns.parallelStream()
                .map(cdn -> {
                    List<Bson> query = createQuery(timestamp, toTimestamp, countryCode, 60 * 24, bucketSize, numberOfBuckets, cdn, statName);
                    return queryDatabase(query, bucketSize, cdn);
                })
                .map(histogramCompletableFuture -> {
                    try {
                        return histogramCompletableFuture.get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException("Failed querying for histogram.", e);
                    }
                })
                .forEach(histogramList::addHistogram);

        if (log.isTraceEnabled()) {
            log.trace(String.format("Finished histogram list in %sms", Duration.between(start, Instant.now()).toMillis()));
        }

        return histogramList;
    }

    private List<Bson> createQuery(long timestamp, long toTimestamp, String countryCode, int defaultMinutes, int bucketSize, int numberOfBuckets, String cdn, String statName) {
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

        if (StringUtils.isNotBlank(cdn)) {
            query = and(query, eq("cdn", cdn));
        }

        if (StringUtils.isNotBlank(statName)) {
            query = and(query, eq("statName", statName));
        }

        BucketOptions options = new BucketOptions();
        options.defaultBucket("default");
        options.output(new BsonField("count", eq("$sum", 1)));
        return Arrays.asList(
                match(query),
                bucket("$timeTakenMillis",
                        HistogramList.buckets(bucketSize, numberOfBuckets),
                        options)
        );
    }

    private CompletableFuture<Histogram> queryDatabase(List<Bson> query, Integer bucketSize, String histogramName) {
        CompletableFuture<Histogram> result = new CompletableFuture<>();

        Instant start = Instant.now();
        if (log.isTraceEnabled()) {
            log.trace(String.format("Started histogram query for %s...", histogramName));
        }

        Histogram histogram = new Histogram(histogramName);
        mongoClient
                .getDatabase("SydneyJavaMeetup")
                .getCollection("PerfStats")
                .aggregate(query)
                .forEach(stat -> {
                    Object id = stat.get("_id");
                    if (id instanceof Integer) {
                        Integer lowerBound = (Integer) id;
                        Integer upperBound = lowerBound + bucketSize;
                        Integer count = stat.getInteger("count");
                        String name = String.format("%d-%d", lowerBound, upperBound);

                        HistogramBucket histogramBucket = new HistogramBucket(name, upperBound, count);
                        histogram.addBucket(histogramBucket);
                        histogram.incrementTotal(count);
                    }
                }, (aVoid, throwable) -> {
                    if (throwable != null) {
                        result.completeExceptionally(throwable);
                    } else {
                        if (log.isTraceEnabled()) {
                            log.trace(String.format("Finished histogram for %s in %sms", histogramName, Duration.between(start, Instant.now()).toMillis()));
                        }

                        result.complete(histogram);
                    }
                });

        return result;
    }
}
