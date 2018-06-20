package com.mycodefu;

import com.mycodefu.data.CDN;
import com.mycodefu.data.CdnBucket;
import com.mycodefu.data.CdnSummary;
import com.mycodefu.data.CdnTimes;
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
import java.util.List;
import java.util.stream.IntStream;

public class PerfStatsDataAccess {
    private MongoClient mongoClient;

    public PerfStatsDataAccess(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }

    public void histogramStatsSince(String timestampString, String toTimestampString, String countryCode, String queryCapString, String bucketSizeString, String statName, Handler<AsyncResult<JsonObject>> resultHandler) {
        CdnSummary cdnSummary = new CdnSummary();

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

        List<CDN> cdns = new ArrayList<>();
        cdns.add(CDN.CloudFront);
        boolean includeAliCloud = "CN".equalsIgnoreCase(countryCode);
        if (includeAliCloud) {
            cdns.add(CDN.AliCloud);
        }

        //Create empty buckets
        cdns.forEach(cdn -> {
            CdnTimes cdnTimes = new CdnTimes();
            cdnTimes.setCdn(cdn);
            IntStream.rangeClosed(1, numberOfBuckets).map(bucketNo -> bucketNo * bucketSize).forEach(bucketEnd -> {
                CdnBucket timeBucket = new CdnBucket(String.format("%d-%d", bucketEnd - bucketSize, bucketEnd),
                        bucketEnd,
                        0);
                cdnTimes.addBucket(timeBucket);
            });

            cdnSummary.addCdn(cdnTimes);
        });

        //Fill up the buckets!
        queryStats(timestamp, toTimestamp, countryCode, 60 * 24, statName, cdnSummary, queryCap, bucketSize, includeAliCloud, resultHandler);
    }


    private int roundToBucketEnd(double value, int bucketSize) {
        return (int) (Math.ceil(value / bucketSize) * bucketSize);
    }

    private void queryStats(long timestamp, long toTimestamp, String countryCode, int defaultMinutes, String statName, CdnSummary cdnSummary, Integer queryCap, Integer bucketSize, boolean includeAliCloud, Handler<AsyncResult<JsonObject>> resultHandler) {
        Instant from;
        if (timestamp <= 0) {
            from = Instant.now().minus(defaultMinutes, ChronoUnit.MINUTES);
        } else {
            from = Instant.ofEpochMilli(timestamp);
        }

        JsonObject filters = new JsonObject();
        JsonObject dateFilter = new JsonObject().put("$gte", new JsonObject().put("$date", from.toString()));
        if (toTimestamp > 0) {
            dateFilter.put("$lt", new JsonObject().put("$date", Instant.ofEpochMilli(toTimestamp).toString()));
        }
        filters.put("timestamp", dateFilter);
        if (StringUtils.isNotBlank(countryCode)) {
            filters.put("countryCode", countryCode);
        }
        queryForStats(filters, statName, cdnSummary, queryCap, bucketSize, includeAliCloud, resultHandler);
    }

    private void queryForStats(JsonObject query, String statName, CdnSummary cdnSummary, Integer queryCap, Integer bucketSize, boolean includeAliCloud, Handler<AsyncResult<JsonObject>> resultHandler) {
        System.out.println("Querying for stats:");
        System.out.println(query.encodePrettily());

        JsonObject fields = new JsonObject().put("timestamp", 1).put("stats", 1);
        JsonObject sort = new JsonObject();//.put("timestamp", 1);
        mongoClient.findWithOptions("perfstats", query, new FindOptions().setFields(fields).setSort(sort), result -> {
            if (result.succeeded()) {
                List<JsonObject> resultData = result.result();
                System.out.println(String.format("Found %d stats for query.", resultData.size()));
                List<JsonObject> filteredStats = filterStats(resultData, statName);

                filteredStats.forEach(stat -> {
                    Integer cloudFront = stat.getInteger(CDN.CloudFront.name());
                    if (cloudFront != null && cloudFront < queryCap && cloudFront > 0) {
                        cdnSummary.incrementBucket(CDN.CloudFront, roundToBucketEnd(cloudFront, bucketSize));
                    }
                    if (includeAliCloud) {
                        Integer aliCloud = stat.getInteger(CDN.AliCloud.name());
                        if (aliCloud!=null && aliCloud < queryCap && aliCloud > 0) {
                            cdnSummary.incrementBucket(CDN.AliCloud, roundToBucketEnd(aliCloud, bucketSize));
                        }
                    }
                });

                resultHandler.handle(Future.succeededFuture(JsonObject.mapFrom(cdnSummary)));
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
