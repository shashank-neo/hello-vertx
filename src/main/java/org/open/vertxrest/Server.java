package org.open.vertxrest;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by shwet.s under project vertx-rest. <br/>
 * Created on  20/06/16. <br/>
 * Updated on 20/06/16.  <br/>
 * Updated by shwet.s. <br/>
 */
@SuppressWarnings(value = "unchecked")
public class Server extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(Server.class);
    private final HashMap<String, String> twitterVerticlesMap = new HashMap<>();

    public void start(Future<Void> future) {
        HttpServer server = vertx.createHttpServer();
        Router router = Router.router(vertx);

        router.route(HttpMethod.POST, "/v1/stream/start").produces("application/json")
                .consumes("application/json").handler(routingContext -> {
            HttpServerRequest httpRequest = routingContext.request();
            Map<String, Object> responseMap = new HashMap<>();
            HttpServerResponse response = routingContext.response();
            httpRequest.bodyHandler(body -> {
                JsonObject bodyJson = new JsonObject(body.toString());
                JsonArray searchKeyWords = bodyJson.getJsonArray("filters");
                if (searchKeyWords.isEmpty()) {
                    responseMap.put("message", "filters are blank.");
                    response.setStatusCode(400).setChunked(true).write(new JsonObject(responseMap)
                            .toString()).end();
                } else {
                    TwitterVerticle twitterVerticle = new TwitterVerticle(searchKeyWords.getList(), "");
                    DeploymentOptions options = new DeploymentOptions().setWorker(true);
                    vertx.deployVerticle(twitterVerticle, options, result -> {
                        if (result.succeeded()) {
                            String id = UUID.randomUUID().toString();
                            twitterVerticlesMap.put(id, result.result());
                            responseMap.put("message", "created a stream with search filters " + searchKeyWords
                                    .toString());
                            responseMap.put("id", id);
                            response.setStatusCode(201).setChunked(true).write((new JsonObject(responseMap))
                                    .toString()).end();
                        } else {
                            responseMap.put("message", "Failed to start stream.");
                            response.setStatusCode(500).setChunked(true).write(new JsonObject(responseMap)
                                    .toString()).end();
                        }
                    });
                }

            });
        });

        router.route(HttpMethod.PUT, "/v1/stream/:streamId/stop").produces("application/json")
                .consumes("application/json").handler(routingContext -> {
            HttpServerRequest request = routingContext.request();
            Map<String, Object> responseMap = new HashMap<>();
            HttpServerResponse response = routingContext.response();
            String streamId = request.getParam("streamId");
            if (twitterVerticlesMap.containsKey(streamId)) {
                String verticleId = twitterVerticlesMap.get(streamId);
                vertx.undeploy(verticleId, res -> {
                    if(res.succeeded()) {
                        responseMap.put("message", "Stopped stream successfully");
                        response.setStatusCode(200).setChunked(true).write(new JsonObject(responseMap).toString())
                                .end();
                    } else {
                        responseMap.put("message", "failed to stop the stream. " + res.result());
                        response.setStatusCode(500).setChunked(true).write(new JsonObject(responseMap).toString())
                                .end();
                    }
                });
            } else {
                responseMap.put("message", "Wrong stream id.");
                response.setStatusCode(400).setChunked(true).write(new JsonObject(responseMap).toString()).end();
            }
        });

        router.route("/static/*").handler(StaticHandler.create());

        server.requestHandler(router::accept).listen(9999, res -> {
            if (res.succeeded()) {
                log.info("Server started listening on 9999");
                future.complete();
            } else {
                future.fail("Failed to start server");
            }
        });

    }

    public void stop() {

    }
}
