package com.ian.springbootredis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @Autowired
    private RedisTemplate redisTemplate;

    @GetMapping("/hello")
    public String hello() {

        int i = 0;
        Object s = redisTemplate.opsForValue().get("hello");
        if (s == null) {
            i = 1;
            redisTemplate.opsForValue().set("hello", i);
        } else {
            i = Integer.valueOf(s.toString());
            i++;
        }
        redisTemplate.opsForValue().set("hello", String.valueOf(i));
        return "Hello World! I have been seen : " + i + " times";
    }

    @GetMapping("/redis/{value}")
    public String setRedis(@PathVariable(value = "value") String value) {
        redisTemplate.opsForValue().set("data", value);
        return "set OK !";
    }

    @GetMapping("/redis")
    public String getRedis() {
        return redisTemplate.opsForValue().get("data").toString();
    }

    @GetMapping("/redis/{key}/{value}")
    public String SetredisKeyValue(@PathVariable(value = "key") String key,
            @PathVariable(value = "value") String value) {
        redisTemplate.opsForValue().set(key, value);
        return "set OK !";
    }

    @GetMapping("/redis/{key}")
    public String GetredisKey(@PathVariable(value = "key") String key) {
        return redisTemplate.opsForValue().get(key).toString();
    }

}
