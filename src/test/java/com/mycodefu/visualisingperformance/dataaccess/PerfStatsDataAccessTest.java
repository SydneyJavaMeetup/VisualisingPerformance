package com.mycodefu.visualisingperformance.dataaccess;

import com.mongodb.async.client.MongoCollection;
import com.mongodb.client.model.InsertOneModel;
import com.mycodefu.visualisingperformance.data.HistogramList;
import org.apache.logging.log4j.core.util.IOUtils;
import org.bson.Document;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.mycodefu.visualisingperformance.dataaccess.MongoConnection.DATABASE_NAME;
import static org.junit.Assert.*;

public class PerfStatsDataAccessTest {

    /**
     * Note: requires local mongodb with the PerfStats collection in the SydneyJavaMeetup database, e.g. run this from the data directory after unzipping the PerfStats.json.zip file:
     * mongoimport PerfStats.json -d SydneyJavaMeetup
     */
    @Test
    public void histogramStatsSince() throws IOException, InterruptedException {
        InputStream testDataResource = PerfStatsDataAccessTest.class.getResourceAsStream("/test-data.json");
        String testDataString = IOUtils.toString(new InputStreamReader(testDataResource));
        List<InsertOneModel<Document>> testData = Arrays.stream(testDataString.split("\n")).map(Document::parse).map(InsertOneModel::new).collect(Collectors.toList());
        CountDownLatch countDownLatch = new CountDownLatch(1);
        MongoCollection<Document> collection = MongoConnection.get().getDatabase(DATABASE_NAME).getCollection("testcollection");
        collection.drop((aVoid, throwable) -> {
            assertNull(throwable);
            collection.bulkWrite(testData, (bulkWriteResult, throwable2) -> {
                assertNull(throwable2);

                for (int i=0; i < 15; i++) {
                    HistogramList histogramList = new PerfStatsDataAccess(collection, false).histogramStatsSince(
                            "1528736400000",
                            "0",
                            "CN",
                            "3000",
                            "100",
                            "img-large");

                    assertEquals(2, histogramList.getHistograms().size());
                    assertEquals(30, histogramList.getHistograms().get(0).getBuckets().size());
                    assertEquals(30, histogramList.getHistograms().get(1).getBuckets().size());

                    //check bucket 0 had a greater than 0 count
                    assertTrue(histogramList.getHistograms().get(0).getBuckets().get(0).getCount() > 0);
                }
                countDownLatch.countDown();
            });
        });

        countDownLatch.await(30, TimeUnit.SECONDS);
    }
}