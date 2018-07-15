package com.mycodefu.visualisingperformance.dataaccess;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.PutMetricDataResult;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.mongodb.async.client.MongoClient;
import com.mongodb.client.model.BucketOptions;
import com.mycodefu.visualisingperformance.data.Histogram;
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
import java.util.stream.Collectors;

import static com.mongodb.client.model.Aggregates.bucket;
import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Filters.*;

public class PerfStatsDataAccess {
    static final Logger log = LogManager.getLogger(PerfStatsDataAccess.class);

    private MongoClient mongoClient;
    private boolean logMetrics;

    public PerfStatsDataAccess(MongoClient mongoClient, boolean logMetrics) {
        this.mongoClient = mongoClient;
        this.logMetrics = logMetrics;
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
        List<CompletableFuture<Histogram>> futures = cdns.stream().map(cdn -> {
                List<Bson> query = createQuery(timestamp, toTimestamp, countryCode, 60 * 24, bucketSize, numberOfBuckets, cdn, statName);
                return queryDatabase(query, bucketSize, cdn, numberOfBuckets);
            }).collect(Collectors.toList());

        for (CompletableFuture<Histogram> future : futures) {
            try {
                Histogram histogram = future.get();
                histogramList.addHistogram(histogram);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("Failed querying for histogram.", e);
            }
        }

        long totalDurationMillis = Duration.between(start, Instant.now()).toMillis();
        log.info(String.format("Finished histogram list in %sms", totalDurationMillis));
        writeMetric("HistogramListRequest", totalDurationMillis);

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

        //todo: add the range of acceptable values to the search:
        // e.g. "timeTakenMillis": {"$gt": 0, "$lt":3000}

        BucketOptions options = new BucketOptions()
                .defaultBucket("default");
        return Arrays.asList(
                match(query),
                bucket("$timeTakenMillis",
                        HistogramList.buckets(bucketSize, numberOfBuckets),
                        options)
        );
    }

    private CompletableFuture<Histogram> queryDatabase(List<Bson> query, Integer bucketSize, String histogramName, int numberOfBuckets) {
        CompletableFuture<Histogram> result = new CompletableFuture<>();

        Instant start = Instant.now();
        if (log.isTraceEnabled()) {
            log.trace(String.format("Started histogram query for %s...", histogramName));
        }

        Histogram histogram = Histogram.of(bucketSize, numberOfBuckets, histogramName);
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

                        histogram.setBucketCount(upperBound, count);
                        histogram.incrementTotal(count);
                    }
                }, (aVoid, throwable) -> {
                    if (throwable != null) {
                        result.completeExceptionally(throwable);
                    } else {
                        long durationMillis = Duration.between(start, Instant.now()).toMillis();
                        if (log.isTraceEnabled()) {
                            log.trace(String.format("Finished histogram for %s in %sms", histogramName, durationMillis));
                        }

                        writeMetric("HistogramAggregateRequest", durationMillis);

                        result.complete(histogram);
                    }
                });

        return result;
    }

    private void writeMetric(String name, double durationMillis) {
        if (logMetrics) {
            try {
                final AmazonCloudWatch cw =
                        AmazonCloudWatchClientBuilder.defaultClient();

                MetricDatum datum = new MetricDatum()
                        .withMetricName(name)
                        .withUnit(StandardUnit.Milliseconds)
                        .withValue(durationMillis);

                PutMetricDataRequest request = new PutMetricDataRequest()
                        .withNamespace("VISUALISING_PERFORMANCE")
                        .withMetricData(datum);

                PutMetricDataResult putMetricDataResult = cw.putMetricData(request);
                log.info(putMetricDataResult.getSdkResponseMetadata());

            } catch (Exception e) {
                log.error("Failed to write CloudWatch metric.", e);
            }
        }
    }
}
