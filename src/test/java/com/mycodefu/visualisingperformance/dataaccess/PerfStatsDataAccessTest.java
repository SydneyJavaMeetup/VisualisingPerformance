package com.mycodefu.visualisingperformance.dataaccess;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mycodefu.visualisingperformance.data.HistogramList;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PerfStatsDataAccessTest {

    /**
     * Note: requires local mongodb with the PerfStats collection in the SydneyJavaMeetup database, e.g. run this from the data directory after unzipping the PerfStats.json.zip file:
     * mongoimport PerfStats.json -d SydneyJavaMeetup
     */
    @Test
    public void histogramStatsSince() throws JsonProcessingException {
        //warmup
        for (int i=0; i < 15; i++) {
            HistogramList histogramList = new PerfStatsDataAccess(MongoConnection.get(), false).histogramStatsSince(
                    "1529156835987",
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
    }
}