package com.mycodefu;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

import java.nio.charset.StandardCharsets;

public class StatsVisualizerEntryPoint {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        Router router = Router.router(vertx);
        handleWebServices(router);
        handleHtml(router);

        int port = 8090;
        vertx.createHttpServer().requestHandler(router::accept).listen(port);

        System.out.println("Listening on http://localhost:" + port);

    }

    private static void handleWebServices(Router router) {
        router.get("/getHistogram.json").handler(routingContext -> {
            routingContext
                    .response()
                    .putHeader("Content-Type", "application/json")
                    .end(new JsonObject().put("stats", new JsonArray().add("")).encode());
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
