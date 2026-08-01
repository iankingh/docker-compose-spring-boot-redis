package com.ian.springbootredis;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HelloController.class)
class HelloControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StringRedisTemplate redisTemplate;

    @MockBean
    @SuppressWarnings("rawtypes")
    private ValueOperations valueOperations;

    @SuppressWarnings("unchecked")
    private ValueOperations<String, String> ops() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        return (ValueOperations<String, String>) valueOperations;
    }

    @Test
    void hello_firstCall_startsAtOne() throws Exception {
        when(ops().get("hello")).thenReturn(null);

        mockMvc.perform(get("/hello"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("seen : 1 times")));

        verify(valueOperations).set(eq("hello"), eq("1"));
    }

    @Test
    void hello_subsequentCall_increments() throws Exception {
        when(ops().get("hello")).thenReturn("4");

        mockMvc.perform(get("/hello"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("seen : 5 times")));

        verify(valueOperations).set(eq("hello"), eq("5"));
    }

    @Test
    void setRedis_storesFixedDataKey() throws Exception {
        ops();

        mockMvc.perform(get("/redis/set/world"))
                .andExpect(status().isOk())
                .andExpect(content().string("set OK !"));

        verify(valueOperations).set(eq("data"), eq("world"));
    }

    @Test
    void getRedis_returnsValue_whenPresent() throws Exception {
        when(ops().get("data")).thenReturn("world");

        mockMvc.perform(get("/redis/get"))
                .andExpect(status().isOk())
                .andExpect(content().string("world"));
    }

    @Test
    void getRedis_returns404_whenAbsent() throws Exception {
        when(ops().get("data")).thenReturn(null);

        mockMvc.perform(get("/redis/get"))
                .andExpect(status().isNotFound());
    }

    @Test
    void setRedisKeyValue_storesArbitraryKey() throws Exception {
        ops();

        mockMvc.perform(get("/redis/set/mykey/myvalue"))
                .andExpect(status().isOk())
                .andExpect(content().string("set OK !"));

        verify(valueOperations).set(eq("mykey"), eq("myvalue"));
    }

    @Test
    void getRedisKey_returnsValue_whenPresent() throws Exception {
        when(ops().get("mykey")).thenReturn("myvalue");

        mockMvc.perform(get("/redis/get/mykey"))
                .andExpect(status().isOk())
                .andExpect(content().string("myvalue"));
    }

    @Test
    void getRedisKey_returns404_whenAbsent() throws Exception {
        when(ops().get("missing")).thenReturn(null);

        mockMvc.perform(get("/redis/get/missing"))
                .andExpect(status().isNotFound());
    }
}