package com.mycodefu.visualisingperformance.dataaccess;

import com.mycodefu.visualisingperformance.data.HistogramList;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.Assert.*;

public class PerfStatsDataAccessTest {

    @org.junit.Test
    public void histogramStatsSince() throws ExecutionException, InterruptedException {
        Future<HistogramList> histogramListFuture = new PerfStatsDataAccess(MongoConnection.get()).histogramStatsSince(
                "1529156835987",
                "0",
                "CN",
                "3000",
                "100",
                "img-large");

        HistogramList value = histogramListFuture.get();
        System.out.println(value);
    }
}