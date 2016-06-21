package org.open.vertxrest;

import io.vertx.core.Vertx;

/**
 * Created by shwet.s under project vertx-rest. <br/>
 * Created on  20/06/16. <br/>
 * Updated on 20/06/16.  <br/>
 * Updated by shwet.s. <br/>
 */
public class Main {
    private static Vertx vertx;
    public static void main(String[] args) {
        //How do we pass config??
        vertx = Vertx.vertx();
        //vertx.deployVerticle(Server.class.getName());
        vertx.deployVerticle(WebSocketServer.class.getName());
    }
}
