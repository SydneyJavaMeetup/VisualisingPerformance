package com.mycodefu;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;

import java.nio.charset.StandardCharsets;

public class StatsVisualizerEntryPoint {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        MongoClient client = MongoClient.createShared(vertx, new JsonObject().put("connection_string", "mongodb://localhost:27017/SydneyJavaMeetup"));
        Router router = Router.router(vertx);
        handleWebServices(router, client);
        handleHtml(router);

        int port = 8090;
        vertx.createHttpServer().requestHandler(router::accept).listen(port);

        System.out.println("Listening on http://localhost:" + port);

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
