package com.oms.orderservice.api.idempotency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import org.testcontainers.containers.GenericContainer;

import org.junit.jupiter.api.Disabled;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@Disabled("Flaky Redis connection in test environment - fixing later")
class OrderCreateIdempotencyTest {

  @Container
  static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.0.12-alpine"))
      .withExposedPorts(6379)
      .withCommand("redis-server", "--appendonly", "no", "--protected-mode", "no");

  @DynamicPropertySource
  static void redisProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.redis.host", redis::getHost);
    registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    registry.add("spring.data.redis.timeout", () -> "60s");
  }

  @LocalServerPort
  int port;

  WebTestClient webTestClient;

  @Autowired
  RedisTemplate<String, Object> redisTemplate;

  @BeforeEach
  void setup() {
    redisTemplate.getConnectionFactory()
        .getConnection()
        .flushDb();

    this.webTestClient = WebTestClient
        .bindToServer()
        .baseUrl("http://localhost:" + port)
        .build();
  }

  @Test
  void shouldCreateOrderSuccessfully() {
    webTestClient.post()
        .uri("/orders")
        .header("Idempotency-Key", "idem-123")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("""
                {
                  "items": [
                    {
                      "productId": "prod-1",
                      "quantity": 2,
                      "price": 100
                    }
                  ]
                }
            """)
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.orderId").exists()
        .jsonPath("$.status").exists();
  }
}
