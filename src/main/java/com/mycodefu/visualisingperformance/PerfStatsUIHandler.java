package com.mycodefu.visualisingperformance;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.mycodefu.visualisingperformance.data.ApiGatewayResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Scanner;

public class PerfStatsUIHandler implements RequestHandler<Map<String, Object>, ApiGatewayResponse> {

    static final Logger log = LogManager.getLogger(PerfStatsUIHandler.class);

    @Override
    public ApiGatewayResponse handleRequest(Map<String, Object> input, Context context) {
        try {
            log.info("event:" + JsonUtil.mapper.writeValueAsString(input));
        } catch (JsonProcessingException ignored) {}

        try {
            int response;
            String index_html;
            try {
                InputStream resourceAsStream = PerfStatsUIHandler.class.getResourceAsStream("/index.html");
                index_html = new Scanner(resourceAsStream, StandardCharsets.UTF_8.name()).useDelimiter("\\A").next();
                response = 200;
            } catch (Exception e) {
                index_html = "File not found";
                response = 404;
            }
            return ApiGatewayResponse.builder()
                    .setStatusCode(response)
                    .setObjectBody(index_html)
                    .addHeader("Content-Type", "text/html")
                    .build();

        } catch (Exception e) {
            log.error("Error serving HTML", e);
            return ApiGatewayResponse.builder()
                    .setStatusCode(500)
                    .setObjectBody(e.getMessage())
                    .addHeader("X-Powered-By", "AWS Lambda & serverless")
                    .build();
        }
    }
}
