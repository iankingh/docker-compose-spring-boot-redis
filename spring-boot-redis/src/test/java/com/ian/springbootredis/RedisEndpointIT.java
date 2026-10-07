package com.ian.springbootredis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Timeout(40)
class RedisEndpointIT {
    @Value("${local.server.port}")
    private int port;

    @Autowired
    private StringRedisTemplate redis;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();
    private final String key = "endpoint-it-" + UUID.randomUUID();
    private boolean ownsFixedKeys;

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", () -> required("integration.redis.host"));
        registry.add("spring.data.redis.port", () -> required("integration.redis.port"));
        registry.add("spring.data.redis.database", () -> required("integration.redis.database"));
        registry.add("spring.data.redis.timeout", () -> "5s");
    }

    private static String required(String name) {
        String value = System.getProperty(name);
        if (value == null || value.isBlank() || value.startsWith("${")) {
            throw new IllegalArgumentException("Explicit test Redis setting required: " + name);
        }
        return value;
    }

    @BeforeEach
    void requireUnusedFixedKeys() {
        assertFalse(redis.hasKey("hello"), "Use an isolated Redis database without the hello key");
        assertFalse(redis.hasKey("data"), "Use an isolated Redis database without the data key");
        ownsFixedKeys = true;
    }

    @AfterEach
    void removeOnlyOwnedKeys() {
        if (ownsFixedKeys) {
            redis.delete(List.of("hello", "data", key));
        }
    }

    @Test
    void concurrentHttpRequestsIncrementAtomically() throws Exception {
        int requests = 64;
        var executor = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<String>> responses = new ArrayList<>();
        try {
            for (int i = 0; i < requests; i++) {
                responses.add(executor.submit(() -> {
                    assertTrue(start.await(5, TimeUnit.SECONDS));
                    HttpResponse<String> response = get("/hello");
                    assertEquals(200, response.statusCode());
                    return response.body();
                }));
            }
            start.countDown();
            Set<String> actual = new HashSet<>();
            for (Future<String> response : responses) {
                actual.add(response.get(20, TimeUnit.SECONDS));
            }
            Set<String> expected = new HashSet<>();
            for (int i = 1; i <= requests; i++) {
                expected.add("Hello World! I have been seen : " + i + " times");
            }
            assertEquals(expected, actual);
            assertEquals(Integer.toString(requests), redis.opsForValue().get("hello"));
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void fixedAndNamedKeysRoundTripAndMissingKeysReturn404() throws Exception {
        assertEquals(404, get("/redis/get").statusCode());
        assertEquals(404, get("/redis/get/" + key).statusCode());
        HttpResponse<String> fixed = get("/redis/set/world");
        assertEquals(200, fixed.statusCode());
        assertEquals("set OK !", fixed.body());
        HttpResponse<String> fixedRead = get("/redis/get");
        assertEquals(200, fixedRead.statusCode());
        assertEquals("world", fixedRead.body());
        assertEquals("world", redis.opsForValue().get("data"));
        assertEquals(200, get("/redis/set/" + key + "/value").statusCode());
        HttpResponse<String> namedRead = get("/redis/get/" + key);
        assertEquals(200, namedRead.statusCode());
        assertEquals("value", namedRead.body());
        assertEquals("value", redis.opsForValue().get(key));
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }
}
