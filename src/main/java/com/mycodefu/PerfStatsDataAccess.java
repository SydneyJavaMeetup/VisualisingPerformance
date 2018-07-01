package com.mycodefu;

import com.mycodefu.data.HistogramBucket;
import com.mycodefu.data.HistogramList;
import com.mycodefu.data.Histogram;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PerfStatsDataAccess {
    private MongoClient mongoClient;

    public PerfStatsDataAccess(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }

    public void histogramStatsSince(String timestampString, String toTimestampString, String countryCode, String queryCapString, String bucketSizeString, String statName, Handler<AsyncResult<JsonObject>> resultHandler) {

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
        HistogramList histogramList = createEmptyHistogram(bucketSize, numberOfBuckets, cdns);

        //Fill up the buckets!
        JsonObject query = createQuery(timestamp, toTimestamp, countryCode, 60 * 24);
        queryDatabase(query, statName, histogramList, queryCap, bucketSize, includeAliCloud, resultHandler);
    }

    private HistogramList createEmptyHistogram(int bucketSize, int numberOfBuckets, List<String> groups) {
        HistogramList histogramList = new HistogramList();
        histogramList.addHistograms(
            groups.stream().map(cdn -> {
                Histogram histogram = new Histogram();
                histogram.setGroup(cdn);
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


    private int roundToBucketEnd(double value, int bucketSize) {
        return (int) (Math.ceil(value / bucketSize) * bucketSize);
    }

    private JsonObject createQuery(long timestamp, long toTimestamp, String countryCode, int defaultMinutes) {
        JsonObject query = new JsonObject();

        Instant from;
        if (timestamp <= 0) {
            from = Instant.now().minus(defaultMinutes, ChronoUnit.MINUTES);
        } else {
            from = Instant.ofEpochMilli(timestamp);
        }

        JsonObject dateFilter = new JsonObject().put("$gte", new JsonObject().put("$date", from.toString()));
        if (toTimestamp > 0) {
            dateFilter.put("$lt", new JsonObject().put("$date", Instant.ofEpochMilli(toTimestamp).toString()));
        }
        query.put("timestamp", dateFilter);
        if (StringUtils.isNotBlank(countryCode)) {
            query.put("countryCode", countryCode);
        }

        return query;
    }

    private void queryDatabase(JsonObject query, String statName, HistogramList histogramList, Integer queryCap, Integer bucketSize, boolean includeAliCloud, Handler<AsyncResult<JsonObject>> resultHandler) {
        System.out.println("Querying for stats:");
        System.out.println(query.encodePrettily());

        JsonObject fields = new JsonObject().put("timestamp", 1).put("stats", 1);
        mongoClient.findWithOptions("perfstats", query, new FindOptions().setFields(fields), result -> {
            if (result.succeeded()) {
                List<JsonObject> resultData = result.result();
                System.out.println(String.format("Found %d stats for query.", resultData.size()));
                List<JsonObject> filteredStats = filterStats(resultData, statName);

                filteredStats.forEach(stat -> {
                    Integer cloudFront = stat.getInteger("CloudFront");
                    if (cloudFront != null && cloudFront < queryCap && cloudFront > 0) {
                        histogramList.incrementBucket("CloudFront", roundToBucketEnd(cloudFront, bucketSize));
                    }
                    if (includeAliCloud) {
                        Integer aliCloud = stat.getInteger("AliCloud");
                        if (aliCloud!=null && aliCloud < queryCap && aliCloud > 0) {
                            histogramList.incrementBucket("AliCloud", roundToBucketEnd(aliCloud, bucketSize));
                        }
                    }
                });

                resultHandler.handle(Future.succeededFuture(JsonObject.mapFrom(histogramList)));
            } else {
                resultHandler.handle(Future.failedFuture(result.cause()));
            }
        });
    }

    /**
     * Remove anything other than the large image stats.
     */
    private List<JsonObject> filterStats(List<JsonObject> stats, String statName) {
        List<JsonObject> results = new ArrayList<>(stats.size());
        for (JsonObject stat : stats) {
            JsonObject resultStat = new JsonObject();
            resultStat.put("stats", new JsonArray());
            resultStat.put("timestamp", stat.getJsonObject("timestamp"));
            JsonArray statsList = stat.getJsonArray("stats");
            if (statsList != null) {
                for (Object innerStatObject : statsList) {
                    JsonObject innerStat = (JsonObject) innerStatObject;
                    if (innerStat.getString("name").equals(statName)) {
                        resultStat.put(innerStat.getString("cdn"), innerStat.getInteger("timeTakenMillis"));
                    }
                }
                results.add(resultStat);
            }
        }
        return results;
    }
}
