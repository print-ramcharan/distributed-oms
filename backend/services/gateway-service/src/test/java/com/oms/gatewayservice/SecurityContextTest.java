package com.oms.gatewayservice;

import com.oms.gatewayservice.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest
@AutoConfigureWebTestClient
class SecurityContextTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private JwtUtil jwtUtil;

    @org.springframework.boot.test.context.TestConfiguration
    @org.springframework.web.bind.annotation.RestController
    static class TestConfig {
        @org.springframework.web.bind.annotation.GetMapping("/test/secure")
        public String secureEndpoint() {
            return "secure";
        }
    }

    @Test
    void shouldReturnUnauthorizedWhenNoTokenProvided() {
        webTestClient.get().uri("/test/secure")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldReturnOkWhenValidTokenProvided() {
        String token = jwtUtil.generateToken("user-123");

        webTestClient.get().uri("/test/secure")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("secure");
    }
}
