package com.mycodefu;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;

public class StatsVisualizerEntryPoint {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        String connectionString = getEnvSetting("MONGO_CONNECTION_STRING", "mongodb://localhost:27017/SydneyJavaMeetup");
        MongoClient client = MongoClient.createShared(vertx, new JsonObject().put("connection_string", connectionString));
        Router router = Router.router(vertx);
        handleWebServices(router, client);
        handleHtml(router);

        int port = 8090;
        vertx.createHttpServer().requestHandler(router::accept).listen(port);

        System.out.println("Listening on http://localhost:" + port);
        System.out.println("---");
        System.out.println("China comparison 3 days: http://localhost:" + port + "/?countryCode=CN&queryCap=4000&bucketSize=100&statName=img-large&timestamp=1529156835987&toTimestamp=0");
        System.out.println("Germany all time: http://localhost:" + port + "/?countryCode=DE&queryCap=2000&bucketSize=50&statName=img-large&timestamp=1&toTimestamp=0");
        System.out.println("All data: http://localhost:" + port + "/?queryCap=4000&bucketSize=100&statName=img-large&timestamp=1&toTimestamp=0");

    }

    private static String getEnvSetting(String name, String defaultValue) {
        return StringUtils.isNotBlank(System.getenv(name)) ? System.getenv(name) : defaultValue;
    }

    private static void handleWebServices(Router router, MongoClient client) {
        router.get("/getHistogram.json").handler(routingContext -> {
            HttpServerResponse httpServerResponse = routingContext
                    .response()
                    .putHeader("Content-Type", "application/json");

            HttpServerRequest r = routingContext.request();
            new PerfStatsDataAccess(client).histogramStatsSince(
                    r.getParam("timestamp"),
                    r.getParam("toTimestamp"),
                    r.getParam("countryCode"),
                    r.getParam("queryCap"),
                    r.getParam("bucketSize"),
                    r.getParam("statName"),
                    result -> {
                        if (result.succeeded()) {
                            httpServerResponse.end(result.result().encode());
                        } else {
                            httpServerResponse
                                    .setStatusCode(500)
                                    .end(new JsonObject().put("failed", result.cause().getMessage()).encode());
                        }
                    });
        });
    }

    private static void handleHtml(Router router) {
        router.get("/").handler(routingContext -> {
            int response;
            String index_html;
            try {
                index_html = new String(StatsVisualizerEntryPoint.class.getResourceAsStream("/index.html").readAllBytes(), StandardCharsets.UTF_8);
                response = 200;
            } catch (Exception e) {
                index_html = "File not found";
                response = 404;
            }
            routingContext
                    .response()
                    .putHeader("Content-Type", "text/html")
                    .setStatusCode(response)
                    .end(index_html);
        });
    }
}
