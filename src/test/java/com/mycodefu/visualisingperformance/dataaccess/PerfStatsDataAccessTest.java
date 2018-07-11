package com.mycodefu.visualisingperformance.dataaccess;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mycodefu.visualisingperformance.data.HistogramList;
import org.junit.Test;

public class PerfStatsDataAccessTest {

    /**
     * Note: requires local mongodb with the PerfStats collection in the SydneyJavaMeetup database, e.g. run this from the data directory after unzipping the PerfStats.json.zip file:
     * mongoimport PerfStats.json -d SydneyJavaMeetup
     */
    @Test
    public void histogramStatsSince() throws JsonProcessingException {
        //warmup
        for (int i=0; i < 100; i++) {
            HistogramList histogramList = new PerfStatsDataAccess(MongoConnection.get()).histogramStatsSince(
                    "1529156835987",
                    "0",
                    "DE",
                    "3000",
                    "100",
                    "img-large");
        }
    }
}