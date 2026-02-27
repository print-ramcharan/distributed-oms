package com.oms.orderservice;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.test.context.ActiveProfiles;

@Disabled("Docker/DB environment unavailable")
@SpringBootTest
@ActiveProfiles("test")
class OrderServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
