package org.open.vertxrest;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by shwet.s under project vertx-rest. <br/>
 * Created on  21/06/16. <br/>
 * Updated on 21/06/16.  <br/>
 * Updated by shwet.s. <br/>
 */
public class WebSocketServer extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(Server.class);
    private final HashMap<String, String> twitterVerticlesMap = new HashMap<>();

    @Override
    public void start() {
        HttpServer server = vertx.createHttpServer();
        Router router = Router.router(vertx);
        BridgeOptions bridgeOptions = new BridgeOptions().addInboundPermitted(new PermittedOptions().setAddressRegex
                ("messageToServer")).addOutboundPermitted(new PermittedOptions().setAddressRegex
                ("messageToClient_.*"));

        SockJSHandler ebHandler = SockJSHandler.create(vertx).bridge(bridgeOptions);
        router.route("/eventbus/*").handler(ebHandler);
        router.route("/static/*").handler(StaticHandler.create());

        router.route(HttpMethod.POST, "/api/v1/stream/start").produces("application/json").handler(routingContext -> {
            Map<String, Object> responseMap = new HashMap<>();
            HttpServerResponse response = routingContext.response();
            String clientId = UUID.randomUUID().toString();
            responseMap.put("id", clientId);
            responseMap.put("message", "Stream created successfully");
            response.setChunked(true).setStatusCode(201).write(new JsonObject(responseMap).toString()).end();
        });


        router.route(HttpMethod.POST, "/api/v1/stream/:streamId/stop").produces("application/json").handler(routingContext -> {
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
        EventBus eb = vertx.eventBus();
        eb.consumer("messageToServer").handler(objectMessage -> {
            JsonObject messageJson = new JsonObject(objectMessage.body().toString());
            if(messageJson.getString("command").equalsIgnoreCase("start")) {
                String filterString = messageJson.getString("filters");
                String[] filters = filterString.split(",");
                String streamId = messageJson.getString("clientId");
                TwitterVerticle twitterVerticle = new TwitterVerticle(Arrays.asList(filters), streamId);
                vertx.deployVerticle(twitterVerticle, stringAsyncResult -> {
                    JsonObject jsonObject = new JsonObject();
                    if(stringAsyncResult.succeeded()) {
                        twitterVerticlesMap.put(streamId, stringAsyncResult.result());
                        //eb.send("messageToClient_" + streamId, jsonObject.put("message", "Streaming tweets now").put(
                        //        "status", "SUCCESS").toString());
                    } else {
                        //eb.send("messageToClient_" + streamId, jsonObject.put("message", "Streaming failed.").put
                        //        ("status", "SUCCESS").toString());
                    }
                });
                log.info("got flters " + filterString);
            } else {
                log.error("Unknown Command");
            }
        });

        server.websocketHandler(serverWebSocket -> serverWebSocket.handler(serverWebSocket::writeBinaryMessage))
                .requestHandler(router::accept).listen(9999);
    }
}
