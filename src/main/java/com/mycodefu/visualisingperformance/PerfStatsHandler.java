package com.mycodefu.visualisingperformance;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.async.client.MongoCollection;
import com.mycodefu.visualisingperformance.data.ApiGatewayResponse;
import com.mycodefu.visualisingperformance.data.HistogramList;
import com.mycodefu.visualisingperformance.dataaccess.MongoConnection;
import com.mycodefu.visualisingperformance.dataaccess.PerfStatsDataAccess;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;

import static com.mycodefu.visualisingperformance.dataaccess.MongoConnection.DATABASE_NAME;
import static com.mycodefu.visualisingperformance.dataaccess.MongoConnection.STATS_COLLECTION_NAME;

public class PerfStatsHandler implements RequestHandler<Map<String, Object>, ApiGatewayResponse> {

    static final Logger log = LogManager.getLogger(PerfStatsHandler.class);

    @Override
    public ApiGatewayResponse handleRequest(Map<String, Object> input, Context context) {
        if (log.isTraceEnabled()) {
            try {
                log.trace("event:" + JsonUtil.mapper.writeValueAsString(input));
            } catch (JsonProcessingException ignored) {
            }
        }
        try {
            Map<String, String> queryStringParameters = input.get("queryStringParameters") == null ? new HashMap<>() : (Map<String, String>)input.get("queryStringParameters");
            MongoCollection<Document> collection = MongoConnection.get()
                    .getDatabase(DATABASE_NAME)
                    .getCollection(STATS_COLLECTION_NAME);
            HistogramList value = new PerfStatsDataAccess(collection, true)
                    .histogramStatsSince(
                        queryStringParameters.get("timestamp"),
                        queryStringParameters.get("toTimestamp"),
                        queryStringParameters.get("countryCode"),
                        queryStringParameters.get("queryCap"),
                        queryStringParameters.get("bucketSize"),
                        queryStringParameters.get("statName")
                    );

            return ApiGatewayResponse.builder()
                    .setStatusCode(200)
                    .setObjectBody(value)
                    .addHeader("Content-Type", "application/json")
                    .build();

        } catch (Exception e) {
            log.error("Error creating histogram", e);
            return ApiGatewayResponse.builder()
                    .setStatusCode(500)
                    .setObjectBody(e.getMessage())
                    .addHeader("X-Powered-By", "AWS Lambda & serverless")
                    .build();
        }
    }
}
