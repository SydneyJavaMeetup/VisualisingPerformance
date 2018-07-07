package com.mycodefu;

import com.mongodb.async.client.MongoClient;
import com.mycodefu.data.HistogramList;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.mongodb.client.model.Filters.*;

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
        Bson query = createQuery(timestamp, toTimestamp, countryCode, 60 * 24);
        return queryDatabase(query, statName, histogramList, queryCap, bucketSize, includeAliCloud);
    }

    private int roundToBucketEnd(double value, int bucketSize) {
        return (int) (Math.ceil(value / bucketSize) * bucketSize);
    }

    private Bson createQuery(long timestamp, long toTimestamp, String countryCode, int defaultMinutes) {
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

        return query;
    }

    private CompletableFuture<HistogramList> queryDatabase(Bson query, String statName, HistogramList histogramList, Integer queryCap, Integer bucketSize, boolean includeAliCloud) {
        CompletableFuture<HistogramList> result = new CompletableFuture<>();

        Bson fields = and(eq("timestamp", 1), eq("stats", 1));
        mongoClient
                .getDatabase("SydneyJavaMeetup")
                .getCollection("perfstats")
                .find(query)
                .projection(fields)
                .map(document -> filterStat(statName, document))
                .forEach(stat -> {
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
        }, (aVoid, throwable) -> {
            if (throwable != null) {
                result.completeExceptionally(throwable);
            } else {
                result.complete(histogramList);
            }
        });

        return result;
    }

    private Document filterStat(String statName, Document stat) {
        Document resultStat = new Document();
        resultStat.put("stats", new ArrayList());
        resultStat.put("timestamp", stat.getDate("timestamp"));
        ArrayList statsList = stat.get("stats", ArrayList.class);
        if (statsList != null) {
            for (Object innerStatObject : statsList) {
                Document innerStat = (Document) innerStatObject;
                if (innerStat.getString("name").equals(statName)) {
                    resultStat.put(innerStat.getString("cdn"), innerStat.getInteger("timeTakenMillis"));
                }
            }
        }
        return resultStat;
    }
}
