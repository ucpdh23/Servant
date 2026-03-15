package es.xan.servantv3.homeautomation;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.templ.thymeleaf.ThymeleafTemplateEngine;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertTrue;

public class HackerNewsTemplateTest {
    private Vertx vertx;
    private ThymeleafTemplateEngine engine;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
        engine = ThymeleafTemplateEngine.create(vertx);
        engine.getThymeleafTemplateEngine().addDialect(new Java8TimeDialect());
    }

    @After
    public void tearDown() {
        vertx.close();
    }

    @Test
    public void renderHackerNewsTemplate() throws Exception {
        // Create a sample list of JsonObject items
        List<JsonObject> items = new ArrayList<>();
        JsonObject it1 = new JsonObject().put("name", "First item").put("url", "https://example.com/1").put("commentsCounter", 123).put("commentsUrl", "https://example.com/1/comments").put("times", 12).put("tags", "[ai, programming, web]");
        JsonObject it2 = new JsonObject().put("name", "Second item").put("url", "https://example.com/2").put("commentsCounter", 432).put("commentsUrl", "https://example.com/2/comments").put("times", 44).put("tags", "[windows, virtualization]");
        items.add(it1);
        items.add(it2);

        Map<String, Object> model = new HashMap<>();
        model.put("items", items);

        CompletableFuture<Buffer> fut = new CompletableFuture<>();

        engine.render(model, "templates/hackernews.html", ar -> {
            if (ar.succeeded()) {
                fut.complete(ar.result());
            } else {
                fut.completeExceptionally(ar.cause());
            }
        });

        Buffer buffer = fut.get();

        // write output to a file so you can inspect it
        File out = new File("target/test-hackernews.html");
        try (BufferedWriter w = new BufferedWriter(new FileWriter(out))) {
            w.write(buffer.toString());
        }

        String html = buffer.toString();
        // Basic assertions: the titles should appear in the rendered HTML
        assertTrue(html.contains("First item"));
        assertTrue(html.contains("Second item"));

        // also assert that the link for first item is present
        assertTrue(html.contains("https://example.com/1"));
    }
}
