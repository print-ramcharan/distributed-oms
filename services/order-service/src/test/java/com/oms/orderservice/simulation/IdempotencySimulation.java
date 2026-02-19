package com.oms.orderservice.simulation;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.Duration;
import java.util.UUID;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class IdempotencySimulation extends Simulation {

    HttpProtocolBuilder httpProtocol = http
            .baseUrl("http://localhost:8080")
            .acceptHeader("application/json")
            .contentTypeHeader("application/json");

    // Scenario: Send SAME request twice with SAME Idempotency-Key
    ScenarioBuilder scn = scenario("Idempotency Load Test")
            .exec(session -> session.set("idempotencyKey", UUID.randomUUID().toString()))
            .exec(
                    http("First Request")
                            .post("/api/orders")
                            .header("Idempotency-Key", "#{idempotencyKey}")
                            .body(StringBody("""
                                    { "customerEmail": "idempotent@example.com", "items": [] }
                                    """)).asJson()
                            .check(status().is(200)))
            .pause(1)
            .exec(
                    http("Second Request (Duplicate)")
                            .post("/api/orders")
                            .header("Idempotency-Key", "#{idempotencyKey}")
                            .body(StringBody("""
                                    { "customerEmail": "idempotent@example.com", "items": [] }
                                    """)).asJson()
                            .check(status().is(200))
            // Verify it's not a 409 or 500, but a clean 200 (cached response)
            );

    {
        setUp(
                scn.injectOpen(atOnceUsers(20))).protocols(httpProtocol);
    }
}
