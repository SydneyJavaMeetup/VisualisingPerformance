package com.mycodefu.visualisingperformance.dataaccess;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.client.model.InsertOneModel;
import com.mycodefu.visualisingperformance.JsonUtil;
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

    private static MongoCollection<Document> collection = intializeTestData();

    /**
     * Note: requires local mongodb with the PerfStats collection in the SydneyJavaMeetup database, e.g. run this from the data directory after unzipping the PerfStats.json.zip file:
     * mongoimport PerfStats.json -d SydneyJavaMeetup
     */
    @Test
    public void histogramStatsSince() throws IOException, InterruptedException {


        HistogramList histogramList = null;
        for (int i=0; i < 15; i++) {
            histogramList = new PerfStatsDataAccess(collection, false).histogramStatsSince(
                    "1528736400000",
                    "0",
                    "CN",
                    "3000",
                    "100",
                    "img-large");
        }

        assertNotNull("Failed to return a result", histogramList.getHistograms());
        assertEquals(2, histogramList.getHistograms().size());
        assertEquals(30, histogramList.getHistograms().get(0).getBuckets().size());
        assertEquals(30, histogramList.getHistograms().get(1).getBuckets().size());

        String jsonOutput;
        try {
            jsonOutput = JsonUtil.mapper.writeValueAsString(histogramList);
            System.out.println(jsonOutput);
        } catch (JsonProcessingException e) {
            jsonOutput = null;
        }
        assertNotNull("Failed to serialise to JSON", jsonOutput);
        assertEquals("Failed to serialise to expected JSON", "{\"histograms\":[{\"group\":\"CloudFront\",\"buckets\":[{\"name\":\"0-100\",\"upperBound\":100,\"count\":13},{\"name\":\"100-200\",\"upperBound\":200,\"count\":36},{\"name\":\"200-300\",\"upperBound\":300,\"count\":85},{\"name\":\"300-400\",\"upperBound\":400,\"count\":85},{\"name\":\"400-500\",\"upperBound\":500,\"count\":145},{\"name\":\"500-600\",\"upperBound\":600,\"count\":175},{\"name\":\"600-700\",\"upperBound\":700,\"count\":155},{\"name\":\"700-800\",\"upperBound\":800,\"count\":114},{\"name\":\"800-900\",\"upperBound\":900,\"count\":109},{\"name\":\"900-1000\",\"upperBound\":1000,\"count\":111},{\"name\":\"1000-1100\",\"upperBound\":1100,\"count\":111},{\"name\":\"1100-1200\",\"upperBound\":1200,\"count\":89},{\"name\":\"1200-1300\",\"upperBound\":1300,\"count\":72},{\"name\":\"1300-1400\",\"upperBound\":1400,\"count\":70},{\"name\":\"1400-1500\",\"upperBound\":1500,\"count\":64},{\"name\":\"1500-1600\",\"upperBound\":1600,\"count\":58},{\"name\":\"1600-1700\",\"upperBound\":1700,\"count\":48},{\"name\":\"1700-1800\",\"upperBound\":1800,\"count\":34},{\"name\":\"1800-1900\",\"upperBound\":1900,\"count\":29},{\"name\":\"1900-2000\",\"upperBound\":2000,\"count\":28},{\"name\":\"2000-2100\",\"upperBound\":2100,\"count\":20},{\"name\":\"2100-2200\",\"upperBound\":2200,\"count\":14},{\"name\":\"2200-2300\",\"upperBound\":2300,\"count\":15},{\"name\":\"2300-2400\",\"upperBound\":2400,\"count\":14},{\"name\":\"2400-2500\",\"upperBound\":2500,\"count\":13},{\"name\":\"2500-2600\",\"upperBound\":2600,\"count\":11},{\"name\":\"2600-2700\",\"upperBound\":2700,\"count\":8},{\"name\":\"2700-2800\",\"upperBound\":2800,\"count\":7},{\"name\":\"2800-2900\",\"upperBound\":2900,\"count\":8},{\"name\":\"2900-3000\",\"upperBound\":3000,\"count\":9}],\"totalCount\":1750},{\"group\":\"AliCloud\",\"buckets\":[{\"name\":\"0-100\",\"upperBound\":100,\"count\":72},{\"name\":\"100-200\",\"upperBound\":200,\"count\":233},{\"name\":\"200-300\",\"upperBound\":300,\"count\":196},{\"name\":\"300-400\",\"upperBound\":400,\"count\":221},{\"name\":\"400-500\",\"upperBound\":500,\"count\":229},{\"name\":\"500-600\",\"upperBound\":600,\"count\":172},{\"name\":\"600-700\",\"upperBound\":700,\"count\":133},{\"name\":\"700-800\",\"upperBound\":800,\"count\":82},{\"name\":\"800-900\",\"upperBound\":900,\"count\":64},{\"name\":\"900-1000\",\"upperBound\":1000,\"count\":48},{\"name\":\"1000-1100\",\"upperBound\":1100,\"count\":35},{\"name\":\"1100-1200\",\"upperBound\":1200,\"count\":39},{\"name\":\"1200-1300\",\"upperBound\":1300,\"count\":26},{\"name\":\"1300-1400\",\"upperBound\":1400,\"count\":31},{\"name\":\"1400-1500\",\"upperBound\":1500,\"count\":29},{\"name\":\"1500-1600\",\"upperBound\":1600,\"count\":26},{\"name\":\"1600-1700\",\"upperBound\":1700,\"count\":29},{\"name\":\"1700-1800\",\"upperBound\":1800,\"count\":16},{\"name\":\"1800-1900\",\"upperBound\":1900,\"count\":16},{\"name\":\"1900-2000\",\"upperBound\":2000,\"count\":11},{\"name\":\"2000-2100\",\"upperBound\":2100,\"count\":12},{\"name\":\"2100-2200\",\"upperBound\":2200,\"count\":9},{\"name\":\"2200-2300\",\"upperBound\":2300,\"count\":7},{\"name\":\"2300-2400\",\"upperBound\":2400,\"count\":9},{\"name\":\"2400-2500\",\"upperBound\":2500,\"count\":5},{\"name\":\"2500-2600\",\"upperBound\":2600,\"count\":15},{\"name\":\"2600-2700\",\"upperBound\":2700,\"count\":8},{\"name\":\"2700-2800\",\"upperBound\":2800,\"count\":7},{\"name\":\"2800-2900\",\"upperBound\":2900,\"count\":7},{\"name\":\"2900-3000\",\"upperBound\":3000,\"count\":4}],\"totalCount\":1791}]}", jsonOutput);

        //check bucket 0 had a greater than 0 count
        assertTrue(histogramList.getHistograms().get(0).getBuckets().get(0).getCount() > 0);
    }

    @Test
    public void histogramStatsSince_useDefaults() throws IOException, InterruptedException {
        HistogramList histogramList = new PerfStatsDataAccess(collection, false).histogramStatsSince(
                "1528736400000",
                "",
                "",
                "",
                "",
                "");
        assertNotNull(histogramList);
        assertEquals(1, histogramList.getHistograms().size());
    }

    private static MongoCollection<Document> intializeTestData() {
        InputStream testDataResource = PerfStatsDataAccessTest.class.getResourceAsStream("/test-data.json");
        String testDataString = null;
        try {
            testDataString = IOUtils.toString(new InputStreamReader(testDataResource));
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<InsertOneModel<Document>> testData = Arrays.stream(testDataString.split("\n")).map(Document::parse).map(InsertOneModel::new).collect(Collectors.toList());
        CountDownLatch countDownLatch = new CountDownLatch(1);
        MongoCollection<Document> collection = MongoConnection.get().getDatabase(DATABASE_NAME).getCollection("testcollection");
        collection.drop((aVoid, throwable) -> {
            assertNull(throwable);
            collection.bulkWrite(testData, (bulkWriteResult, throwable2) -> {
                assertNull(throwable2);
                countDownLatch.countDown();
            });
        });
        try {
            countDownLatch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return collection;
    }
}