package com.mycodefu.visualisingperformance.dataaccess;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mycodefu.visualisingperformance.JsonUtil;
import com.mycodefu.visualisingperformance.data.HistogramList;

public class PerfStatsDataAccessTest {

    @org.junit.Test
    public void histogramStatsSince() throws JsonProcessingException {
        HistogramList histogramList = new PerfStatsDataAccess(MongoConnection.get()).histogramStatsSince(
                "1529156835987",
                "0",
                "CN",
                "3000",
                "100",
                "img-large");

        System.out.println(JsonUtil.mapper.writeValueAsString(histogramList));
    }
}