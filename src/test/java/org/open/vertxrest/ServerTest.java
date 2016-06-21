package org.open.vertxrest;

import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by shwet.s under project vertx-rest. <br/>
 * Created on  20/06/16. <br/>
 * Updated on 20/06/16.  <br/>
 * Updated by shwet.s. <br/>
 */
@RunWith(VertxUnitRunner.class)
public class ServerTest {
    private Vertx vertx;

    @Before
    public void setUp(TestContext context) {
        vertx = Vertx.vertx();
        vertx.deployVerticle(Server.class.getName(), context.asyncAssertSuccess());
    }

    @After
    public void tearDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void testMyServer(TestContext testContext) {
        final Async  async = testContext.async();
        vertx.createHttpClient().getNow(9999, "localhost", "/test", response -> {
            response.handler(body -> {
                testContext.assertTrue(body.toString().contains("vertx"));
                async.complete();
            });
        });
    }
}
