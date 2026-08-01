package com.ian.springbootredis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    private final StringRedisTemplate redisTemplate;

    public HelloController(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/hello")
    public String hello() {
        String s = redisTemplate.opsForValue().get("hello");
        int i = (s == null) ? 1 : Integer.parseInt(s) + 1;
        redisTemplate.opsForValue().set("hello", String.valueOf(i));
        return "Hello World! I have been seen : " + i + " times";
    }

    // Set the fixed "data" key: GET /redis/set/{value}
    @GetMapping("/redis/set/{value}")
    public String setRedis(@PathVariable String value) {
        redisTemplate.opsForValue().set("data", value);
        return "set OK !";
    }

    // Get the fixed "data" key: GET /redis/get
    @GetMapping("/redis/get")
    public ResponseEntity<String> getRedis() {
        String val = redisTemplate.opsForValue().get("data");
        if (val == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(val);
    }

    // Set any key: GET /redis/set/{key}/{value}
    @GetMapping("/redis/set/{key}/{value}")
    public String setRedisKeyValue(@PathVariable String key, @PathVariable String value) {
        redisTemplate.opsForValue().set(key, value);
        return "set OK !";
    }

    // Get any key: GET /redis/get/{key}
    @GetMapping("/redis/get/{key}")
    public ResponseEntity<String> getRedisKey(@PathVariable String key) {
        String val = redisTemplate.opsForValue().get(key);
        if (val == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(val);
    }

}
